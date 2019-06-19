package org.fok.actuator.token;

import com.google.protobuf.ByteString;
import onight.tfw.ntrans.api.annotation.ActorRequire;

import java.math.BigInteger;

import org.fok.actuator.AbstractTransactionActuator;
import org.fok.actuator.AbstractUnTransferActuator;
import org.fok.actuator.config.ActuatorConfig;
import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionResultHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.*;
import org.fok.core.model.Account.AccountInfo.Builder;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionData.OwnerTokenData;
import org.fok.core.model.Transaction.TransactionData.UserTokenData;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;
import org.fok.tools.unit.UnitHelper;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/15 DESC:
 */
public class ActuatorUserToken extends AbstractUnTransferActuator {

	public ActuatorUserToken(IAccountHandler iAccountHandler, ITransactionResultHandler iTransactionHandler,
			ICryptoHandler iCryptoHandler, BlockInfo currentBlock) {
		super(iAccountHandler, iTransactionHandler, iCryptoHandler, currentBlock);
		// TODO Auto-generated constructor stub
	}

	@Override
	public ByteString onExecute(TransactionInfo transactionInfo) throws Exception {
		TransactionInput input = transactionInfo.getBody().getInput();
		AccountInfo.Builder sender = this.getAccount(input.getAddress().toByteArray());
		AccountInfo.Builder lockAccount = this
				.getAccount(this.iCryptoHandler.hexStrToBytes(ActuatorConfig.lock_account_address));
		UserTokenData oUserTokenData = transactionInfo.getBody().getData().getUserTokenData();
		AccountInfo.Builder user = this.getAccount(oUserTokenData.getAddress().toByteArray());

		this.iAccountHandler.setNonce(sender, input.getNonce() + 1);
		this.iAccountHandler.subBalance(sender, fee);

		if (oUserTokenData.getOpCode() == UserTokenData.UserTokenOpCode.FREEZE) {
			this.iAccountHandler.subBalance(sender, ActuatorConfig.token_freeze_lock_balance);

			this.iAccountHandler.subTokenBalance(user, oUserTokenData.getToken().toByteArray(),
					BytesHelper.bytesToBigInteger(oUserTokenData.getAmount().toByteArray()));
			this.iAccountHandler.addTokenFreezeBalance(user, oUserTokenData.getToken().toByteArray(),
					BytesHelper.bytesToBigInteger(oUserTokenData.getAmount().toByteArray()));

			this.iAccountHandler.addBalance(lockAccount, ActuatorConfig.token_freeze_lock_balance);

			this.putAccount(lockAccount);
			this.putAccount(user);
		} else if (oUserTokenData.getOpCode() == UserTokenData.UserTokenOpCode.UNFREEZE) {
			this.iAccountHandler.subBalance(sender, ActuatorConfig.token_unfreeze_lock_balance);
			this.iAccountHandler.subTokenFreezeBalance(user, oUserTokenData.getToken().toByteArray(),
					BytesHelper.bytesToBigInteger(oUserTokenData.getAmount().toByteArray()));
			this.iAccountHandler.addTokenBalance(user, oUserTokenData.getToken().toByteArray(),
					BytesHelper.bytesToBigInteger(oUserTokenData.getAmount().toByteArray()));

			this.iAccountHandler.addBalance(lockAccount, ActuatorConfig.token_unfreeze_lock_balance);

			this.putAccount(lockAccount);
			this.putAccount(user);
		}
		this.putAccount(sender);
		return ByteString.EMPTY;
	}

	@Override
	public void onPrepareExecute(TransactionInfo transactionInfo) throws Exception {
		super.onPrepareExecute(transactionInfo);

		TransactionInput oInput = transactionInfo.getBody().getInput();
		AccountInfo.Builder sender = this.getAccount(oInput.getAddress().toByteArray());
		AccountInfo.Builder tokenAccount = this.getAccount(this.iAccountHandler.tokenValueAddress());
		UserTokenData oUserTokenData = transactionInfo.getBody().getData().getUserTokenData();
		AccountInfo.Builder user = this.getAccount(oUserTokenData.getAddress().toByteArray());

		if (oUserTokenData.getToken() == null || oUserTokenData.getToken().equals(ByteString.EMPTY)) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, token name must not be empty"));
		} else if (oUserTokenData.getToken().size() > 32) {
			throw new TransactionParameterInvalidException(String.format("parameter invalid, token name invalid"));
		} else if (BytesHelper.bytesToBigInteger(oUserTokenData.getAmount().toByteArray())
				.compareTo(BigInteger.ZERO) < 0) {
			throw new TransactionParameterInvalidException(String.format("parameter invalid, token amount invalid"));
		} else {
			TokenValue.Builder oTokenValue = this.iAccountHandler
					.getToken(tokenAccount, oUserTokenData.getToken().toByteArray()).toBuilder();

			if (!oTokenValue.getAddress().equals(oInput.getAddress())) {
				throw new TransactionParameterInvalidException("parameter invalid, invalid token");
			}
		}

		if (oUserTokenData.getOpCode() == UserTokenData.UserTokenOpCode.FREEZE) {
			if (this.iAccountHandler.getTokenBalance(user, oUserTokenData.getToken().toByteArray())
					.compareTo(BytesHelper.bytesToBigInteger(oUserTokenData.getAmount().toByteArray())) < 0) {
				throw new TransactionParameterInvalidException("parameter invalid, no enouth token to freeze");
			}

			if (this.iAccountHandler.getBalance(sender)
					.compareTo(ActuatorConfig.token_freeze_lock_balance.add(fee)) < 0) {
				throw new TransactionParameterInvalidException("parameter invalid, no enouth deposit to freeze");
			}
		} else if (oUserTokenData.getOpCode() == UserTokenData.UserTokenOpCode.UNFREEZE) {
			if (this.iAccountHandler.getTokenFreezeBalance(user, oUserTokenData.getToken().toByteArray())
					.compareTo(BytesHelper.bytesToBigInteger(oUserTokenData.getAmount().toByteArray())) < 0) {
				throw new TransactionParameterInvalidException("parameter invalid, no enouth token to unfreeze");
			}

			if (this.iAccountHandler.getBalance(sender)
					.compareTo(ActuatorConfig.token_unfreeze_lock_balance.add(fee)) < 0) {
				throw new TransactionParameterInvalidException("parameter invalid, no enouth deposit to freeze");
			}
		}
	}
}