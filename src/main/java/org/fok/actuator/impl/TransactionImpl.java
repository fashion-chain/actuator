package org.fok.actuator.impl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.fok.actuator.*;
import org.fok.actuator.contract.ActuatorCallContract;
import org.fok.actuator.contract.ActuatorCreateContract;
import org.fok.actuator.cryptotoken.ActuatorCreateCryptoToken;
import org.fok.actuator.enums.ActuatorTypeEnum;
import org.fok.actuator.token.ActuatorOwnerToken;
import org.fok.actuator.token.ActuatorUserToken;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionExecutorHandler;
import org.fok.core.api.ITransactionResultHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Account.AccountInfo.Builder;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionData;
import org.fok.core.model.Transaction.TransactionInfo;
import org.fok.tools.bytes.BytesHashMap;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/14 DESC:
 */
@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "Transaction_Impl")
@Slf4j
@Data
public class TransactionImpl implements ActorService {
	public static ITransactionExecutorHandler getActuator(IAccountHandler iAccountHandler,
			ITransactionResultHandler iTransactionHandler, ICryptoHandler iCryptoHandler, TransactionInfo transaction,
			BlockInfo oCurrentBlock) {
		ITransactionExecutorHandler transactionExecutorHandler = null;
		TransactionData oTransactionData = transaction.getBody().getData();
		if (oTransactionData == null) {
			transactionExecutorHandler = new ActuatorDefault(iAccountHandler, iTransactionHandler, iCryptoHandler,
					oCurrentBlock);
		} else
			switch (oTransactionData.getType()) {
			case PUBLICCONTRACT:
				transactionExecutorHandler = new ActuatorCreateContract(iAccountHandler, iTransactionHandler,
						iCryptoHandler, oCurrentBlock);
				break;
			case PUBLICCRYPTOTOKEN:
				transactionExecutorHandler = new ActuatorCreateCryptoToken(iAccountHandler, iTransactionHandler,
						iCryptoHandler, oCurrentBlock);
				break;
			case OWNERTOKEN:
				transactionExecutorHandler = new ActuatorOwnerToken(iAccountHandler, iTransactionHandler,
						iCryptoHandler, oCurrentBlock);
				break;
			case USERTOKEN:
				transactionExecutorHandler = new ActuatorUserToken(iAccountHandler, iTransactionHandler, iCryptoHandler,
						oCurrentBlock);
				break;
			case CALLCONTRACT:
				transactionExecutorHandler = new ActuatorCallContract(iAccountHandler, iTransactionHandler,
						iCryptoHandler, oCurrentBlock);
				break;
			case UNRECOGNIZED:
				return null;
			default:
				return null;
			}
		return transactionExecutorHandler;
	}
}
