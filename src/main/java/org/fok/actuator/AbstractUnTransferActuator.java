package org.fok.actuator;

import org.fok.actuator.exception.TransactionParameterInvalidException;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionResultHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo.Builder;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.tools.bytes.BytesHashMap;

public class AbstractUnTransferActuator extends AbstractTransactionActuator {

	public AbstractUnTransferActuator(IAccountHandler iAccountHandler, ITransactionResultHandler iTransactionHandler,
			ICryptoHandler iCryptoHandler, BlockInfo currentBlock) {
		super(iAccountHandler, iTransactionHandler, iCryptoHandler, currentBlock);
	}

	@Override
	public void onPrepareExecute(TransactionInfo transactionInfo) throws Exception {
		this.reset();
		super.onPrepareExecute(transactionInfo);

		if (transactionInfo.getBody().getOutputsCount() > 0) {
			throw new TransactionParameterInvalidException("parameter invalid, output in tx body must null");
		}
	}
}
