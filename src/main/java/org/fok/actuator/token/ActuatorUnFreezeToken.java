package org.fok.actuator.token;

import com.google.protobuf.ByteString;
import org.fok.actuator.AbstractTransactionActuator;
import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Account.AccountTokenValue;
import org.fok.core.model.Account.AccountValue;
import org.fok.core.model.Block;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;

import java.math.BigInteger;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/15 DESC:
 */
public class ActuatorUnFreezeToken extends AbstractTransactionActuator {

	public ActuatorUnFreezeToken(IAccountHandler iAccountHandler, ITransactionHandler iTransactionHandler,
			Block.BlockInfo blockInfo, ICryptoHandler iCryptoHandler) {
		super(iAccountHandler, iTransactionHandler, blockInfo, iCryptoHandler);
	}

	@Override
	public ByteString onExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {
		TransactionInput oInput = transactionInfo.getBody().getInputs();
		AccountInfo.Builder sender = accounts.get(oInput.getAddress().toByteArray());

		this.iAccountHandler.subTokenBalance(sender, oInput.getToken().toByteArray(),
				BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray()));
		this.iAccountHandler.addTokenFreezeBalance(sender, oInput.getToken().toByteArray(),
				BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray()));
		this.iAccountHandler.setNonce(sender, oInput.getNonce() + 1);

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

		AccountInfo.Builder sender = accounts.get(oInput.getAddress().toByteArray());
		BigInteger tokenFreezeBalance = this.iAccountHandler.getTokenFreezeBalance(sender, oInput.getToken().toByteArray());
		
		BigInteger bi = BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray());
		if (bi.compareTo(BigInteger.ZERO) < 0) {
			throw new TransactionParameterInvalidException("parameter invalid, amount must large than 0");
		}

		if (tokenFreezeBalance.compareTo(bi) >= 0) {
		} else {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, sender freeze balance less than %s",
							BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray())));
		}

		int nonce = this.iAccountHandler.getNonce(sender);
		if (nonce > oInput.getNonce()) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, sender nonce %s is not equal with transaction nonce %s", nonce,
							oInput.getNonce()));
		}
	}
}
