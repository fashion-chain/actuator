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
import org.fok.actuator.cryptotoken.ActuatorCryptoTokenTransaction;
import org.fok.actuator.enums.ActuatorTypeEnum;
import org.fok.actuator.token.ActuatorBurnToken;
import org.fok.actuator.token.ActuatorCreateToken;
import org.fok.actuator.token.ActuatorLockTokenTransaction;
import org.fok.actuator.token.ActuatorMintToken;
import org.fok.actuator.token.ActuatorTokenTransaction;
import org.fok.actuator.unionaccount.ActuatorCreateUnionAccount;
import org.fok.actuator.unionaccount.ActuatorUnionAccountTokenTransaction;
import org.fok.actuator.unionaccount.ActuatorUnionAccountTransaction;
import org.fok.core.api.IAccountHandler;
import org.fok.core.api.ITransactionExecutorHandler;
import org.fok.core.api.ITransactionHandler;
import org.fok.core.cryptoapi.ICryptoHandler;
import org.fok.core.model.Block.BlockInfo;
import org.fok.core.model.Transaction.TransactionInfo;

/**
 * Email: king.camulos@gmail.com Date: 2018/11/14 DESC:
 */
@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "Transaction_Impl")
@Slf4j
@Data
public class TransactionImpl implements ActorService {
	private IAccountHandler iAccountHandler;
	private ITransactionHandler iTransactionHandler;
	private ICryptoHandler iCryptoHandler;

	public TransactionImpl(IAccountHandler iAccountHandler, ITransactionHandler iTransactionHandler,
			ICryptoHandler iCryptoHandler) {
		this.iAccountHandler = iAccountHandler;
		this.iTransactionHandler = iTransactionHandler;
		this.iCryptoHandler = iCryptoHandler;
	}

	public ITransactionExecutorHandler getActuator(TransactionInfo transaction, BlockInfo oCurrentBlock) {
		ITransactionExecutorHandler transactionExecutorHandler;
		int transactionType = transaction.getBody().getType();
		switch (ActuatorTypeEnum.transf(transactionType)) {
		case TYPE_CreateUnionAccount:
			transactionExecutorHandler = new ActuatorCreateUnionAccount(this.iAccountHandler, this.iTransactionHandler,
					oCurrentBlock, this.iCryptoHandler);
			break;
		case TYPE_TokenTransaction:
			transactionExecutorHandler = new ActuatorTokenTransaction(this.iAccountHandler, this.iTransactionHandler,
					oCurrentBlock, this.iCryptoHandler);
			break;
		case TYPE_UnionAccountTransaction:
			transactionExecutorHandler = new ActuatorUnionAccountTransaction(this.iAccountHandler,
					this.iTransactionHandler, oCurrentBlock, this.iCryptoHandler);
			break;
		case TYPE_CryptoTokenTransaction:
			transactionExecutorHandler = new ActuatorCryptoTokenTransaction(this.iAccountHandler,
					this.iTransactionHandler, oCurrentBlock, this.iCryptoHandler);
			break;
		case TYPE_LockTokenTransaction:
			transactionExecutorHandler = new ActuatorLockTokenTransaction(this.iAccountHandler,
					this.iTransactionHandler, oCurrentBlock, this.iCryptoHandler);
			break;
		case TYPE_CreateContract:
			transactionExecutorHandler = new ActuatorCreateContract(this.iAccountHandler, this.iTransactionHandler,
					oCurrentBlock, this.iCryptoHandler);
			break;
		case TYPE_CreateToken:
			transactionExecutorHandler = new ActuatorCreateToken(this.iAccountHandler, this.iTransactionHandler,
					oCurrentBlock, this.iCryptoHandler);
			break;
		case TYPE_CallContract:
			transactionExecutorHandler = new ActuatorCallContract(this.iAccountHandler, this.iTransactionHandler,
					oCurrentBlock, this.iCryptoHandler);
			break;
		case TYPE_CreateCryptoToken:
			transactionExecutorHandler = new ActuatorCreateCryptoToken(this.iAccountHandler, this.iTransactionHandler,
					oCurrentBlock, this.iCryptoHandler);
			break;
		case TYPE_UnionAccountTokenTransaction:
			transactionExecutorHandler = new ActuatorUnionAccountTokenTransaction(this.iAccountHandler,
					this.iTransactionHandler, oCurrentBlock, this.iCryptoHandler);
			break;
		case TYPE_MintToken:
			transactionExecutorHandler = new ActuatorMintToken(this.iAccountHandler, this.iTransactionHandler,
					oCurrentBlock, this.iCryptoHandler);
			break;
		case TYPE_BurnToken:
			transactionExecutorHandler = new ActuatorBurnToken(this.iAccountHandler, this.iTransactionHandler,
					oCurrentBlock, this.iCryptoHandler);
			break;
		default:
			transactionExecutorHandler = new ActuatorDefault(this.iAccountHandler, this.iTransactionHandler,
					oCurrentBlock, this.iCryptoHandler);
			break;
		}

		return transactionExecutorHandler;
	}
}
