package org.fok.actuator.token;

import com.google.protobuf.ByteString;
import org.fok.actuator.AbstractTransactionActuator;
import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Account.AccountValue;
import org.fok.core.model.Account.TokenValue;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.core.model.Transaction.TransactionOutput;
import org.fok.tools.bytes.BytesComparisons;
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/15 DESC:
 */
public class ActuatorFreezeToken extends AbstractTransactionActuator {

	public ActuatorFreezeToken(IAccountHandler iAccountHandler, ITransactionHandler iTransactionHandler,
			BlockInfo blockInfo, ICryptoHandler crypto) {
		super(iAccountHandler, iTransactionHandler, blockInfo, crypto);
	}

	@Override
	public ByteString onExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {
		TransactionInput oInput = transactionInfo.getBody().getInputs();
		AccountInfo.Builder sender = accounts.get(oInput.getAddress().toByteArray());

		this.iAccountHandler.setNonce(sender, oInput.getNonce() + 1);
		this.iAccountHandler.subTokenBalance(sender, oInput.getToken().toByteArray(),
				BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray()));
		this.iAccountHandler.addTokenFreezeBalance(sender, oInput.getToken().toByteArray(),
				BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray()));

		return ByteString.EMPTY;
	}

	@Override
	public void onPrepareExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {

		if (transactionInfo.getBody().getInputs() == null) {
			throw new TransactionParameterInvalidException("parameter invalid, inputs must be only one");
		}

		if (transactionInfo.getBody().getOutputsCount() != 0) {
			throw new TransactionParameterInvalidException("parameter invalid, outputs must be null");
		}

		TransactionInput oInput = transactionInfo.getBody().getInputs();

		AccountInfo.Builder sender = accounts.get(this.iCryptoHandler.bytesToHexStr(oInput.getAddress().toByteArray()));
		AccountValue.Builder senderAccountValue = sender.getValue().toBuilder();
		BigInteger bi = BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray());

		// token的发行方可以冻结
		byte[] token = oInput.getToken().toByteArray();
		boolean isPublisher = false;

		TokenValue tv = this.iAccountHandler.getToken(
				accounts.get(this.iCryptoHandler.bytesToHexStr(this.iAccountHandler.tokenValueAddress())), token);
		if (!BytesComparisons.equal(tv.getAddress().toByteArray(), oInput.getAddress().toByteArray())) {
			throw new TransactionParameterInvalidException("parameter invalid, only publisher can freeze token");
		}

		BigInteger totalAmount = BigInteger.ZERO;
		List<String> outAddress = new ArrayList();
		for (TransactionOutput to : transactionInfo.getBody().getOutputsList()) {
			String address = this.iCryptoHandler.bytesToHexStr(to.getAddress().toByteArray());
			if (outAddress.contains(address)) {
				throw new TransactionParameterInvalidException("parameter invalid, duplicate output address");
			}
			outAddress.add(address);
			BigInteger tokenBalance = this.iAccountHandler.getTokenBalance(accounts.get(address), token);
			if (tokenBalance.compareTo(BytesHelper.bytesToBigInteger(to.getAmount().toByteArray())) < 0) {
				throw new TransactionParameterInvalidException("parameter invalid, no enouth token to freeze");
			}
			totalAmount.add(tokenBalance);
		}

		if (totalAmount.compareTo(BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray())) != 0) {
			throw new TransactionParameterInvalidException("parameter invalid, transaction value not equal ");
		}

		int nonce = sender.getValue().getNonce();
		if (nonce > oInput.getNonce()) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, sender nonce %s is not equal with transaction nonce %s", nonce,
							oInput.getNonce()));
		}
	}
}