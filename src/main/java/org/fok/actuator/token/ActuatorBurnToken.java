package org.fok.actuator.token;

import com.google.protobuf.ByteString;
import onight.tfw.ntrans.api.annotation.ActorRequire;

import java.math.BigInteger;

import org.fok.actuator.AbstractTransactionActuator;
import org.fok.actuator.config.ActuatorConfig;
import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.*;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.tools.bytes.BytesComparisons;
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;
import org.fok.tools.unit.UnitHelper;

public class ActuatorBurnToken extends AbstractTransactionActuator {
	public ActuatorBurnToken(IAccountHandler iAccountHandler, ITransactionHandler iTransactionHandler,
			BlockInfo blockInfo, ICryptoHandler iCryptoHandler) {
		super(iAccountHandler, iTransactionHandler, blockInfo, iCryptoHandler);
	}

	@Override
	public ByteString onExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {
		TransactionInput input = transactionInfo.getBody().getInputs();
		AccountInfo.Builder sender = accounts.get(input.getAddress().toByteArray());

		iAccountHandler.setNonce(sender, input.getNonce() + 1);
		iAccountHandler.subBalance(sender, ActuatorConfig.token_burn_lock_balance);
		iAccountHandler.subTokenBalance(sender, input.getToken().toByteArray(),
				BytesHelper.bytesToBigInteger(input.getAmount().toByteArray()));

		// set token owner
		AccountInfo.Builder tokenRecordAddress = accounts.get(this.iAccountHandler.tokenValueAddress());
		TokenValue.Builder oTokenValue = this.iAccountHandler
				.getToken(tokenRecordAddress, input.getToken().toByteArray()).toBuilder();
		oTokenValue.setTotal(ByteString.copyFrom(BytesHelper
				.bigIntegerToBytes(BytesHelper.bytesToBigInteger(oTokenValue.getTotal().toByteArray()).subtract(
						UnitHelper.fromWei(BytesHelper.bytesToBigInteger(input.getAmount().toByteArray()))))));

		TokenValueHistory.Builder oTokenValueHistory = TokenValueHistory.newBuilder();
		oTokenValueHistory.setContent("B");
		oTokenValueHistory.setTotal(oTokenValue.getTotal());
		oTokenValueHistory.setTimestamp(transactionInfo.getBody().getTimestamp());
		oTokenValue.addHistory(oTokenValueHistory);

		this.iAccountHandler.putToken(tokenRecordAddress, oTokenValue.getToken().toByteArray(), oTokenValue.build());

		AccountInfo.Builder locker = accounts.get(iAccountHandler.lockBalanceAddress());
		iAccountHandler.addBalance(locker, ActuatorConfig.token_burn_lock_balance);

		accounts.put(tokenRecordAddress.getAddress().toByteArray(), tokenRecordAddress);
		accounts.put(locker.getAddress().toByteArray(), locker);
		accounts.put(sender.getAddress().toByteArray(), sender);

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

		if (oInput.getToken() == null || oInput.getToken().equals(ByteString.EMPTY)) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, token name must not be empty"));
		}

		String token = iCryptoHandler.bytesToHexStr(oInput.getToken().toByteArray());
		if (!token.toUpperCase().equals(token)) {
			throw new TransactionParameterInvalidException(String.format("parameter invalid, token name invalid"));
		}

		AccountInfo.Builder sender = accounts.get(oInput.getAddress().toByteArray());
		BigInteger totalBalance = iAccountHandler.getBalance(sender);
		if (totalBalance.compareTo(ActuatorConfig.token_burn_lock_balance) == -1) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, not enough deposit to create token"));
		}

		AccountInfo.Builder tokenRecordAddress = accounts.get(this.iAccountHandler.tokenValueAddress());
		TokenValue oTokenValue = this.iAccountHandler.getToken(tokenRecordAddress, oInput.getToken().toByteArray());

		if (!oTokenValue.getAddress().equals(oInput.getAddress())) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, token %s not exists", token));
		}

		BigInteger totalTokenBalance = iAccountHandler.getTokenBalance(sender, oInput.getToken().toByteArray());
		if (totalTokenBalance.compareTo(BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray())) == -1) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, not enough amount token to burn"));
		}

		int nonce = sender.getValue().getNonce();
		if (nonce > oInput.getNonce()) {
			throw new TransactionParameterInvalidException(
					String.format("sender nonce %s is not equal with transaction nonce %s", nonce, oInput.getNonce()));
		}
	}
}
