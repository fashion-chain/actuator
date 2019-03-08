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
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;
import org.fok.tools.unit.UnitHelper;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/15 DESC:
 */
public class ActuatorMintToken extends AbstractTransactionActuator {
	@ActorRequire(name = "BlockChain_Config", scope = "global")
	ActuatorConfig actuatorConfig;

	public ActuatorMintToken(IAccountHandler iAccountHandler, ITransactionHandler iTransactionHandler,
			BlockInfo blockInfo, ICryptoHandler iCryptoHandler) {
		super(iAccountHandler, iTransactionHandler, blockInfo, iCryptoHandler);
	}

	@Override
	public ByteString onExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {
		TransactionInput input = transactionInfo.getBody().getInputs();
		AccountInfo.Builder sender = accounts.get(input.getAddress().toByteArray());
		AccountInfo.Builder tokenAccount = accounts.get(iAccountHandler.tokenValueAddress());

		this.iAccountHandler.subBalance(sender, ActuatorConfig.token_mint_lock_balance);
		this.iAccountHandler.addTokenBalance(sender, input.getToken().toByteArray(),
				BytesHelper.bytesToBigInteger(input.getAmount().toByteArray()));
		this.iAccountHandler.setNonce(sender, input.getNonce() + 1);

		TokenValue.Builder oTokenValue = this.iAccountHandler.getToken(sender, input.getToken().toByteArray())
				.toBuilder();
		oTokenValue.setTotal(ByteString.copyFrom(
				BytesHelper.bigIntegerToBytes(BytesHelper.bytesToBigInteger(oTokenValue.getTotal().toByteArray())
						.add(UnitHelper.fromWei(BytesHelper.bytesToBigInteger(input.getAmount().toByteArray()))))));

		TokenValueHistory.Builder oTokenValueHistory = TokenValueHistory.newBuilder();
		oTokenValueHistory.setContent("C");
		oTokenValueHistory.setTotal(oTokenValue.getTotal());
		oTokenValueHistory.setTimestamp(transactionInfo.getBody().getTimestamp());
		oTokenValue.addHistory(oTokenValueHistory);

		this.iAccountHandler.putToken(tokenAccount, oTokenValue.getToken().toByteArray(), oTokenValue.build());

		AccountInfo.Builder locker = accounts.get(iAccountHandler.lockBalanceAddress());
		this.iAccountHandler.addBalance(locker, ActuatorConfig.token_mint_lock_balance);

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

		String token = this.iCryptoHandler.bytesToHexStr(oInput.getToken().toByteArray());
		if (!token.toUpperCase().equals(token)) {
			throw new TransactionParameterInvalidException(String.format("parameter invalid, token name invalid"));
		}

		AccountInfo.Builder sender = accounts.get(oInput.getAddress().toByteArray());

		BigInteger balance = this.iAccountHandler.getBalance(sender);

		if (balance.compareTo(ActuatorConfig.token_mint_lock_balance) == -1) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, not enough deposit %s to create token",
							ActuatorConfig.token_mint_lock_balance));
		}

		BigInteger tokenBalance = this.iAccountHandler.getTokenBalance(sender, oInput.getToken().toByteArray());
		if (tokenBalance.compareTo(BigInteger.ZERO) < 0) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, token %s not exists", token));
		}

		int nonce = this.iAccountHandler.getNonce(sender);
		if (nonce > oInput.getNonce()) {
			throw new TransactionParameterInvalidException(
					String.format("sender nonce %s is not equal with transaction nonce %s", nonce, oInput.getNonce()));
		}
	}
}