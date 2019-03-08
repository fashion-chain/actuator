package org.fok.actuator.token;

import com.google.protobuf.ByteString;
import onight.tfw.ntrans.api.annotation.ActorRequire;
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
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;
import org.fok.tools.unit.UnitHelper;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/15 DESC:
 */
public class ActuatorCreateToken extends AbstractTransactionActuator {
	@ActorRequire(name = "BlockChain_Config", scope = "global")
	ActuatorConfig actuatorConfig;

	public ActuatorCreateToken(IAccountHandler iAccountHandler, ITransactionHandler iTransactionHandler,
			BlockInfo blockInfo, ICryptoHandler iCryptoHandler) {
		super(iAccountHandler, iTransactionHandler, blockInfo, iCryptoHandler);
	}

	@Override
	public ByteString onExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {
		TransactionInput input = transactionInfo.getBody().getInputs();
		AccountInfo.Builder sender = accounts.get(input.getAddress().toByteArray());

		this.iAccountHandler.setNonce(sender, input.getNonce() + 1);
		this.iAccountHandler.subBalance(sender, ActuatorConfig.token_create_lock_balance);
		this.iAccountHandler.addTokenBalance(sender, input.getToken().toByteArray(),
				BytesHelper.bytesToBigInteger(input.getAmount().toByteArray()));

		TokenValue.Builder oTokenValue = TokenValue.newBuilder();
		oTokenValue.setAddress(sender.getAddress());
		oTokenValue.setTimestamp(transactionInfo.getBody().getTimestamp());
		oTokenValue.setToken(input.getToken());
		oTokenValue.setTotal(ByteString.copyFrom(BytesHelper.bigIntegerToBytes(
				UnitHelper.fromWei(BytesHelper.bytesToBigInteger(input.getAmount().toByteArray())))));

		TokenValueHistory.Builder oTokenValueHistory = TokenValueHistory.newBuilder();
		oTokenValueHistory.setContent("C");
		oTokenValueHistory.setTotal(oTokenValue.getTotal());
		oTokenValueHistory.setTimestamp(transactionInfo.getBody().getTimestamp());
		oTokenValue.addHistory(oTokenValueHistory);

		AccountInfo.Builder tokenAccount = accounts.get(iAccountHandler.tokenValueAddress());
		this.iAccountHandler.putToken(tokenAccount, input.getToken().toByteArray(), oTokenValue.build());
		accounts.put(tokenAccount.getAddress().toByteArray(), tokenAccount);
		
		AccountInfo.Builder locker = accounts.get(iAccountHandler.lockBalanceAddress());
		this.iAccountHandler.addBalance(locker, ActuatorConfig.token_create_lock_balance);
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

		// if (token.toUpperCase().startsWith("CW")) {
		// throw new
		// TransactionParameterInvalidException(String.format("parameter
		// invalid, token name invalid"));
		// }

		if (!token.toUpperCase().equals(token)) {
			throw new TransactionParameterInvalidException(String.format("parameter invalid, token name invalid"));
		}

		if (token.length() > 16) {
			throw new TransactionParameterInvalidException(String.format("parameter invalid, token name invalid"));
		}

		if (BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray())
				.compareTo(ActuatorConfig.minTokenTotal) == -1
				|| BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray())
						.compareTo(ActuatorConfig.maxTokenTotal) == 1) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, token amount must between %s and %s ",
							UnitHelper.fromWei(ActuatorConfig.minTokenTotal),
							UnitHelper.fromWei(ActuatorConfig.maxTokenTotal)));
		}

		AccountInfo.Builder sender = accounts.get(oInput.getAddress().toByteArray());
		AccountValue.Builder senderAccountValue = sender.getValue().toBuilder();
		if (BytesHelper.bytesToBigInteger(senderAccountValue.getBalance().toByteArray())
				.compareTo(ActuatorConfig.token_create_lock_balance) == -1) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, not enough deposit %s to create token",
							ActuatorConfig.token_create_lock_balance));
		}

		AccountInfo.Builder tokenRecordAccount = accounts.get(this.iAccountHandler.tokenValueAddress());
		TokenValue oTokenValue = this.iAccountHandler.getToken(tokenRecordAccount, oInput.getToken().toByteArray());
		if (oTokenValue != null) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, duplicate token name %s", token));
		}

		int nonce = senderAccountValue.getNonce();
		if (nonce > oInput.getNonce()) {
			throw new TransactionParameterInvalidException(
					String.format("sender nonce %s is not equal with transaction nonce %s", nonce, oInput.getNonce()));
		}
	}
}