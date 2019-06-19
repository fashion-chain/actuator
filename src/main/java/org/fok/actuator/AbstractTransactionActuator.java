package org.fok.actuator;

import com.google.protobuf.ByteString;
import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.actuator.exception.TransactionVerifyException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionExecutorHandler;
import org.fok.core.api.ITransactionResultHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountCryptoToken;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Account.AccountInfo.Builder;
import org.fok.core.model.Account.AccountValue;
import org.fok.core.model.Block;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.core.model.Transaction.TransactionOutput;
import org.fok.core.model.Transaction.TransactionBody;
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class AbstractTransactionActuator implements ITransactionExecutorHandler {
	protected BytesHashMap<Builder> touchAccounts;
	protected IAccountHandler iAccountHandler;
	protected ITransactionResultHandler iTransactionHandler;
	protected Block.BlockInfo blockInfo;
	protected ICryptoHandler iCryptoHandler;
	// 交易的手续费
	protected BigInteger fee;
	// 交易的主币总量
	protected BigInteger totalAmount;
	// 交易的token总量
	protected BytesHashMap<BigInteger> totalTokens;
	// 交易的crypto token总量
	protected BytesHashMap<List<AccountCryptoToken.Builder>> totalCryptoTokens;

	protected void reset() {
		reset(this.iAccountHandler, this.iTransactionHandler, this.iCryptoHandler, this.blockInfo);
	}

	protected Builder getAccount(ByteString address) {
		AccountInfo.Builder oTouchAccount = touchAccounts.get(address.toByteArray());
		if (oTouchAccount == null) {
			return this.iAccountHandler.getAccountOrCreate(address);
		} else {
			return oTouchAccount;
		}
	}

	protected Builder getAccount(byte[] address) {
		return getAccount(ByteString.copyFrom(address));
	}

	protected void putAccount(AccountInfo.Builder oAccount) {
		touchAccounts.put(oAccount.getAddress().toByteArray(), oAccount);
	}

	@Override
	public boolean needSignature() {
		return false;
	}

	@Override
	public BytesHashMap<Builder> getTouchAccount() {
		return this.touchAccounts;
	}

	public AbstractTransactionActuator(IAccountHandler iAccountHandler, ITransactionResultHandler iTransactionHandler,
			ICryptoHandler iCryptoHandler, BlockInfo currentBlock) {
		this.iAccountHandler = iAccountHandler;
		this.iTransactionHandler = iTransactionHandler;
		this.blockInfo = currentBlock;
		this.iCryptoHandler = iCryptoHandler;
		this.touchAccounts = new BytesHashMap<>();
		this.fee = BigInteger.ZERO;
		this.totalAmount = BigInteger.ZERO;
		totalTokens = null;
		totalTokens = new BytesHashMap<>();
		totalCryptoTokens = null;
		totalCryptoTokens = new BytesHashMap<>();
	}

	public void reset(IAccountHandler iAccountHandler, ITransactionResultHandler iTransactionHandler,
			ICryptoHandler iCryptoHandler, BlockInfo currentBlock) {
		this.iAccountHandler = iAccountHandler;
		this.iTransactionHandler = iTransactionHandler;
		this.blockInfo = currentBlock;
		this.iCryptoHandler = iCryptoHandler;
		this.touchAccounts = new BytesHashMap<>();
		this.fee = BigInteger.ZERO;
		this.totalAmount = BigInteger.ZERO;
		totalTokens = null;
		totalTokens = new BytesHashMap<>();
		totalCryptoTokens = null;
		totalCryptoTokens = new BytesHashMap<>();
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
	public ByteString onExecute(TransactionInfo transactionInfo) throws Exception {
		return ByteString.EMPTY;
	}

	@Override
	public void onPrepareExecute(TransactionInfo transactionInfo) throws Exception {
		this.reset();
		TransactionInput oInput = transactionInfo.getBody().getInput();
		AccountInfo.Builder sender = getAccount(oInput.getAddress());
		// 判断发送方账户的nonce
		int nonce = this.iAccountHandler.getNonce(sender);
		if (nonce > oInput.getNonce()) {
			throw new TransactionParameterInvalidException(
					"parameter invalid, sender nonce is large than transaction nonce");
		}

		BigInteger txFee = BytesHelper.bytesToBigInteger(transactionInfo.getBody().getFee().toByteArray());
		if (txFee.compareTo(BigInteger.ZERO) < 0) {
			throw new TransactionParameterInvalidException("parameter invalid, fee must large than 0");
		}
	}

	@Override
	public void onVerifySignature(TransactionInfo transactionInfo) throws Exception {
		TransactionInfo.Builder signatureTx = transactionInfo.toBuilder();
		TransactionBody.Builder txBody = signatureTx.getBodyBuilder();
		byte[] oMultiTransactionEncode = txBody.build().toByteArray();
		byte[] hexPubKey = this.iCryptoHandler.signatureToKey(oMultiTransactionEncode,
				transactionInfo.getSignature().toByteArray());

		if (!this.iCryptoHandler.verify(hexPubKey, oMultiTransactionEncode,
				transactionInfo.getSignature().toByteArray())) {
			throw new TransactionVerifyException(String.format("signature %s verify fail with pubkey",
					this.iCryptoHandler.bytesToHexStr(transactionInfo.getSignature().toByteArray())));
		}
	}
}
