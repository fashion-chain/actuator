package org.fok.actuator;

import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionResultHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo;
import org.fok.core.model.Block.BlockInfo;
import org.fok.tools.bytes.BytesHashMap;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/7 DESC:
 */
public class ActuatorDefault extends AbstractTransferActuator {
	public ActuatorDefault(IAccountHandler iAccountHandler, ITransactionResultHandler iTransactionHandler,
			ICryptoHandler iEncApiHandler, BlockInfo blockInfo) {
		super(iAccountHandler, iTransactionHandler, iEncApiHandler, blockInfo);
	}
}
