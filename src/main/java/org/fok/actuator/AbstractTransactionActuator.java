package org.fok.actuator;

import com.google.protobuf.ByteString;
import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.actuator.exception.TransactionVerifyException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionExecutorHandler;
import org.fok.core.api.ITransactionHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Account.AccountInfo.Builder;
import org.fok.core.model.Account.AccountValue;
import org.fok.core.model.Block;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.core.model.Transaction.TransactionOutput;
import org.fok.core.model.Transaction.TransactionBody;
import org.fok.core.model.Transaction.TransactionSignature;
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractTransactionActuator implements ITransactionExecutorHandler {
	protected Map<String, TransactionInfo> txValues = new HashMap<>();
	protected IAccountHandler iAccountHandler;
	protected ITransactionHandler iTransactionHandler;
	protected Block.BlockInfo blockInfo;
	protected ICryptoHandler iCryptoHandler;

	@Override
	public boolean needSignature() {
		return false;
	}

	public AbstractTransactionActuator(IAccountHandler iAccountHandler, ITransactionHandler iTransactionHandler,
			BlockInfo currentBlock, ICryptoHandler iCryptoHandler) {
		this.iAccountHandler = iAccountHandler;
		this.iTransactionHandler = iTransactionHandler;
		this.blockInfo = currentBlock;
		this.iCryptoHandler = iCryptoHandler;
	}

	public void reset(IAccountHandler iAccountHandler, ITransactionHandler iTransactionHandler, BlockInfo currentBlock,
			ICryptoHandler iCryptoHandler) {
		this.iAccountHandler = iAccountHandler;
		this.iTransactionHandler = iTransactionHandler;
		this.blockInfo = currentBlock;
		this.iCryptoHandler = iCryptoHandler;
		this.txValues.clear();
	}

	@Override
	public void onExecuteDone(TransactionInfo transactionInfo, BlockInfo blockInfo, ByteString result)
			throws Exception {
		iTransactionHandler.setTransactionDone(transactionInfo, blockInfo, result);
	}

	@Override
	public void onExecuteError(TransactionInfo transactionInfo, BlockInfo blockInfo, ByteString result)
			throws Exception {
		iTransactionHandler.setTransactionError(transactionInfo, blockInfo, result);
	}

	@Override
	public ByteString onExecute(TransactionInfo transactionInfo, BytesHashMap<Builder> accounts) throws Exception {
		TransactionInput oInput = transactionInfo.getBody().getInputs();
		AccountInfo.Builder sender = accounts.get(oInput.getAddress().toByteArray());
		iAccountHandler.setNonce(sender, oInput.getNonce() + 1);
		iAccountHandler.subBalance(sender, BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray()));

		for (TransactionOutput oOutput : transactionInfo.getBody().getOutputsList()) {
			AccountInfo.Builder receiver = accounts.get(oOutput.getAddress().toByteArray());

			iAccountHandler.addBalance(receiver, BytesHelper.bytesToBigInteger(oOutput.getAmount().toByteArray()));
		}

		return ByteString.EMPTY;
	}

	@Override
	public void onPrepareExecute(TransactionInfo transactionInfo, BytesHashMap<Builder> accounts) throws Exception {
		BigInteger outputsTotal = BigInteger.ZERO;
		TransactionInput oInput = transactionInfo.getBody().getInputs();
		BigInteger inputAmount = BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray());
		if (inputAmount.compareTo(BigInteger.ZERO) < 0) {
			throw new TransactionParameterInvalidException("parameter invalid, amount must large than 0");
		}

		AccountInfo.Builder sender = accounts.get(oInput.getAddress().toByteArray());
		AccountValue.Builder senderAccountValue = sender.getValue().toBuilder();

		if (senderAccountValue.getAddressCount() > 0) {
			throw new TransactionParameterInvalidException(
					"parameter invalid, union account does not allow to create this transaction");
		}

		BigInteger inputBalance = this.iAccountHandler.getBalance(sender);

		if (inputBalance.compareTo(BigInteger.ZERO) == -1) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, sender %s balance %s less than 0",
							this.iCryptoHandler.bytesToHexStr(sender.getAddress().toByteArray()), inputBalance));
		}
		if (BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray()).compareTo(BigInteger.ZERO) == -1) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, transaction value %s less than 0",
							BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray())));
		}

		if (BytesHelper.bytesToBigInteger(inputBalance.toByteArray()).compareTo(inputAmount) == -1) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, sender balance %s less than %s", inputBalance,
							BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray())));
		}

		int nonce = this.iAccountHandler.getNonce(sender);
		if (nonce > oInput.getNonce()) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, sender nonce %s is not equal with transaction nonce %s", nonce,
							oInput.getNonce()));
		}

		for (TransactionOutput oOutput : transactionInfo.getBody().getOutputsList()) {
			BigInteger outputAmount = BytesHelper.bytesToBigInteger(oOutput.getAmount().toByteArray());
			if (outputAmount.compareTo(BigInteger.ZERO) < 0) {
				throw new TransactionParameterInvalidException("parameter invalid, amount must large than 0");
			}
			outputsTotal = outputsTotal.add(outputAmount);

			BigInteger balance = BytesHelper.bytesToBigInteger(oOutput.getAmount().toByteArray());
			if (balance.compareTo(BigInteger.ZERO) == -1) {
				throw new TransactionParameterInvalidException(
						String.format("parameter invalid, receive balance %s less than 0", balance));
			}
		}

		if (inputAmount.compareTo(outputsTotal) != 0) {
			throw new TransactionParameterInvalidException(String
					.format("parameter invalid, transaction value %s not equal with %s", inputAmount, outputsTotal));
		}
	}

	@Override
	public void onVerifySignature(TransactionInfo transactionInfo, BytesHashMap<Builder> accounts) throws Exception {
		TransactionInfo.Builder signatureTx = transactionInfo.toBuilder();
		TransactionBody.Builder txBody = signatureTx.getBodyBuilder();
		signatureTx.clearHash();

		txBody = txBody.clearSignatures();
		byte[] oMultiTransactionEncode = txBody.build().toByteArray();
		TransactionSignature transactionSignature = transactionInfo.getBody().getSignatures();
		byte[] hexPubKey = this.iCryptoHandler.signatureToKey(oMultiTransactionEncode,
				transactionSignature.getSignature().toByteArray());

		if (!this.iCryptoHandler.verify(hexPubKey, oMultiTransactionEncode,
				transactionSignature.getSignature().toByteArray())) {
			throw new TransactionVerifyException(String.format("signature %s verify fail with pubkey",
					this.iCryptoHandler.bytesToHexStr(transactionSignature.getSignature().toByteArray())));
		}
	}
}
