package org.fok.actuator.token;

import com.google.protobuf.ByteString;
import org.fok.actuator.AbstractTransactionActuator;
import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Account.AccountValue;
import org.fok.core.model.Block;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.core.model.Transaction.TransactionOutput;
import org.fok.tools.bytes.BytesComparisons;
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;

import java.math.BigInteger;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/15 DESC:
 */
public class ActuatorTokenTransaction extends AbstractTransactionActuator {

	public ActuatorTokenTransaction(IAccountHandler iAccountHandler, ITransactionHandler iTransactionHandler,
			Block.BlockInfo blockInfo, ICryptoHandler iCryptoHandler) {
		super(iAccountHandler, iTransactionHandler, blockInfo, iCryptoHandler);
	}

	@Override
	public void onPrepareExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {

		if (transactionInfo.getBody().getInputs() == null) {
			throw new TransactionParameterInvalidException("parameter invalid, inputs must be only one");
		}

		if (transactionInfo.getBody().getOutputsCount() == 0) {
			throw new TransactionParameterInvalidException("parameter invalid, outputs must not be null");
		}

		TransactionInput oInput = transactionInfo.getBody().getInputs();
		if (oInput.getToken() == null || oInput.getToken().equals(ByteString.EMPTY)) {
			throw new TransactionParameterInvalidException(String.format("parameter invalid, token must not be empty"));
		}

		AccountInfo.Builder sender = accounts.get(oInput.getAddress().toByteArray());
		BigInteger tokenBalance = this.iAccountHandler.getTokenBalance(sender, oInput.getToken().toByteArray());

		if (tokenBalance.compareTo(BigInteger.ZERO) == -1) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, sender balance %s less than 0", tokenBalance));
		}
		if (tokenBalance.compareTo(BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray())) < 0) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, sender balance less than %s",
							BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray())));
		}
		if (BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray()).compareTo(BigInteger.ZERO) == -1) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, transaction value %s less than 0",
							BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray())));
		}

		int nonce = this.iAccountHandler.getNonce(sender);
		if (nonce > oInput.getNonce()) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, sender nonce %s is not equal with transaction nonce %s", nonce,
							oInput.getNonce()));
		}

		BigInteger outputsTotal = BigInteger.ZERO;
		for (TransactionOutput oOutput : transactionInfo.getBody().getOutputsList()) {
			if (BytesHelper.bytesToBigInteger(oOutput.getAmount().toByteArray()).compareTo(BigInteger.ZERO) == -1) {
				throw new TransactionParameterInvalidException(
						String.format("parameter invalid, receive balance %s less than 0",
								BytesHelper.bytesToBigInteger(oOutput.getAmount().toByteArray())));
			}
			outputsTotal = BytesHelper.bytesAdd(outputsTotal, oOutput.getAmount().toByteArray());
		}

		if (BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray()).compareTo(outputsTotal) != 0) {
			throw new TransactionParameterInvalidException(String.format(
					"parameter invalid, transaction inputs value not equal with output value [ %s ]", outputsTotal));
		}
	}

	@Override
	public ByteString onExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {
		TransactionInput oInput = transactionInfo.getBody().getInputs();
		AccountInfo.Builder sender = accounts.get(oInput.getAddress().toByteArray());
		this.iAccountHandler.subTokenBalance(sender, oInput.getToken().toByteArray(),
				BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray()));
		this.iAccountHandler.setNonce(sender, oInput.getNonce() + 1);
		for (TransactionOutput oOutput : transactionInfo.getBody().getOutputsList()) {
			AccountInfo.Builder receiver = accounts.get(oOutput.getAddress().toByteArray());

			this.iAccountHandler.addTokenBalance(receiver, oInput.getToken().toByteArray(),
					BytesHelper.bytesToBigInteger(oOutput.getAmount().toByteArray()));
		}
		return ByteString.EMPTY;
	}
}
