package org.fok.actuator.token;

import com.google.protobuf.ByteString;
import org.fok.actuator.AbstractTransactionActuator;
import org.fok.actuator.exception.TransactionExecuteException;
import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Account.AccountTokenValue;
import org.fok.core.model.Account.AccountValue;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.core.model.Transaction.TransactionInput;
import org.fok.tools.bytes.BytesHashMap;
import org.fok.tools.bytes.BytesHelper;

import java.math.BigInteger;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/15 DESC:
 */
public class ActuatorLockTokenTransaction extends AbstractTransactionActuator {

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
		BigInteger tokenBalance = this.iAccountHandler.getTokenBalance(sender, oInput.getToken().toByteArray());
		BigInteger tokerTransfer = BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray());
		if (tokenBalance.compareTo(BigInteger.ZERO) < 0) {
			throw new TransactionParameterInvalidException("parameter invalid, amount must large than 0");
		}

		if (tokenBalance.compareTo(tokerTransfer) >= 0) {
		} else {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, sender balance %s less than %s", tokenBalance, tokerTransfer));
		}

		int nonce = this.iAccountHandler.getNonce(sender);
		if (nonce > oInput.getNonce()) {
			throw new TransactionParameterInvalidException(
					String.format("parameter invalid, sender nonce %s is not equal with transaction nonce %s", nonce,
							oInput.getNonce()));
		}
	}

	@Override
	public ByteString onExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {
		TransactionInput oInput = transactionInfo.getBody().getInputs();
		AccountInfo.Builder sender = accounts.get(oInput.getAddress().toByteArray());

		this.iAccountHandler.subTokenBalance(sender, oInput.getToken().toByteArray(),
				BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray()));
		this.iAccountHandler.addTokenLockedBalance(sender, oInput.getToken().toByteArray(),
				BytesHelper.bytesToBigInteger(oInput.getAmount().toByteArray()));
		this.iAccountHandler.setNonce(sender, oInput.getNonce() + 1);

		accounts.put(sender.getAddress().toByteArray(), sender);

		return ByteString.EMPTY;
	}

	public ActuatorLockTokenTransaction(IAccountHandler iAccountHandler, ITransactionHandler iTransactionHandler,
			BlockInfo blockInfo, ICryptoHandler iCryptoHandler) {
		super(iAccountHandler, iTransactionHandler, blockInfo, iCryptoHandler);
	}

}
