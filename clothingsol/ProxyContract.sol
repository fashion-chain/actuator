pragma solidity ^0.4.22;

contract ControllerContract{
  function setSellerAddr(address _sellerAddr)public;
  function setProducerAddr(address _producerAddr)public;
  function setPlatformSideAddr(address _platformSideAddr)public;
  function setAllAddress(address _sellerAddr,address _producerAddr,address _platformSideAddr) public;
  function setSellerAddr(string _sellerID,address _sellerAddr)public;
  function setProducerAddr(string _producerID,address _producerAddr)public;
  function setSellerInfo(string _clothID,string _data,uint256 _confirmNum,uint256 _estimateTime) public;
  function setOrderInfo(string _oriderID,string _dataInfo,uint256 _shipTimes) public;
  function computerIssue(uint256 clothPrice,uint256 clothsols)public view returns(uint256);
}

contract ProxyContract{
  address public controAddr ;
  ControllerContract controllerContract;

  address owner;

  constructor() public {
    owner = msg.sender;
  }
  //Check if it is the creator
  function isMaster(address checkAddress) private view returns(bool){
    bool sign = false;
    if(owner == checkAddress){
      sign = true;
    }
    return sign;
  }
  //set controller one address
  function setControllerOneAddr(address _controAddr) public {
    if(isMaster(msg.sender)){
      controAddr  = _controAddr;
      controllerContract = ControllerContract(controAddr);
    }
  }
  function setSellerAddr(address _sellerAddr)public{
    if(isMaster(msg.sender)){
      controllerContract.setSellerAddr(_sellerAddr);
    }
  }
  function setProducerAddr(address _producerAddr)public{
    if(isMaster(msg.sender)){
      controllerContract.setProducerAddr(_producerAddr);
    }

  }
  function setPlatformSideAddr(address _platformSideAddr)public{
    if(isMaster(msg.sender)){
      controllerContract.setPlatformSideAddr(_platformSideAddr);
    }
  }
  function setAllAddress(address _sellerAddr,address _producerAddr,address _platformSideAddr) public{
    if(isMaster(msg.sender)){
      controllerContract.setAllAddress(_sellerAddr,_producerAddr,_platformSideAddr);
    }
  }
  function setSellerAddr(string _sellerID,address _sellerAddr)public{
    if(isMaster(msg.sender)){
      controllerContract.setSellerAddr(_sellerID,_sellerAddr);
    }
  }
  function setProducerAddr(string _producerID,address _producerAddr)public{
    if(isMaster(msg.sender)){
      controllerContract.setProducerAddr(_producerID,_producerAddr);
    }
  }
  function setSellerInfo(string _clothID,string _data,uint256 _confirmNum,uint256 _estimateTime) public{
    if(isMaster(msg.sender)){
      controllerContract.setSellerInfo(_clothID,_data,_confirmNum,_estimateTime);
    }
  }
  function setOrderInfo(string _oriderID,string _dataInfo,uint256 _shipTimes) public{
    if(isMaster(msg.sender)){
      controllerContract.setOrderInfo(_oriderID,_dataInfo,_shipTimes);
    }
  }
  function computerIssue(uint256 clothPrice,uint256 clothsols)public view returns(uint256){
    uint256 intNum = 0;
    if(isMaster(msg.sender)){
      intNum = controllerContract.computerIssue(clothPrice,clothsols);
    }
    return intNum;
  }

}
