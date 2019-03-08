package org.fok.actuator;

import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.tools.bytes.BytesHashMap;

import java.util.Map;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/7 DESC:
 */
public class ActuatorDefault extends AbstractTransactionActuator {
	public ActuatorDefault(IAccountHandler iAccountHandler, ITransactionHandler iTransactionHandler,
			BlockInfo blockInfo, ICryptoHandler iEncApiHandler) {
		super(iAccountHandler, iTransactionHandler, blockInfo, iEncApiHandler);
	}

	@Override
	public void onPrepareExecute(TransactionInfo transactionInfo, BytesHashMap<AccountInfo.Builder> accounts)
			throws Exception {

		if (transactionInfo.getBody().getInputs() == null || transactionInfo.getBody().getOutputsCount() == 0) {
			throw new TransactionParameterInvalidException("parameter invalid, inputs or outputs must not be null");
		}

		super.onPrepareExecute(transactionInfo, accounts);
	}
}
