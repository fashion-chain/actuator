pragma solidity ^0.4.22;


contract PlatformSide{
  function setTransferAddress(address _transferAddr) public;
  function getTransferAddress() public view returns(address);
  function setGas(string orderID,uint256 _gasvalue) public;
  function getGas(string orderID) public view returns(uint256);
  function computerIssue(uint256 clothPrice,uint256 clothsols)public view returns(uint256);
}
contract Producer{
  function setEstimateTime(string _clothID,uint256 _estimateTime) public;
  function getEstimateTime(string _clothID) public view returns(uint256);
  function setShipTime(string _orderID,uint256 _shipTime) public;
  function getShipTime(string _orderID) public view returns(uint256);
  function setOrderData(string _orderID,string _dataBase)public;
  function getOrderData(string _orderID) public view returns(string);
  function setProducerAddr(string _producerID,address _producerAddr) public;
  function getProducerAddr(string _producerID) public view returns(address);
  function setConfirmNumber(string clothID,uint256 confirmNum) public;
  function getConfirmNumber(string clothID)public view returns(uint256);
  function computerIssue(uint256 clothPrice,uint256 clothsols)public view returns(uint256);
}
contract Seller{
  function setSellerData(string clothIDAndSellerID,string clothData) public;
  function getSellerData(string clothIDAndSellerID)public view returns(string);
  function setSellerAddress(string sellerID,address sellerAddr) public;
  function getSellerAddress(string sellerID) public view returns(address);
  function computerIssue(uint256 clothPrice,uint256 clothsols)public view returns(uint256);
}

contract ControllerContract{

  address public producerAddr;
  address public sellerAddr;
  address public platformSideAddr;


  PlatformSide platformSideContract;
  Producer producerContract;
  Seller sellerContract;

  address owner;

  constructor() public {
    owner = msg.sender;
  }

  function isMaster(address checkAddress) private view returns(bool){
    bool sign = false;
    if(owner == checkAddress){
      sign = true;
    }
    return sign;
  }
  function setSellerAddr(address _sellerAddr)public {
    if(isMaster(msg.sender)){
      sellerAddr = _sellerAddr;
      sellerContract = Seller(sellerAddr);
    }
  }

  function setProducerAddr(address _producerAddr)public {
    if(isMaster(msg.sender)){
      producerAddr = _producerAddr;
      producerContract = Producer(producerAddr);
    }
  }

  function setPlatformSideAddr(address _platformSideAddr)public {
    if(isMaster(msg.sender)){
      platformSideAddr = _platformSideAddr;
      platformSideContract = PlatformSide(platformSideAddr);
    }
  }
  function setAllAddress(address _sellerAddr,address _producerAddr,address _platformSideAddr) public{
    if(isMaster(msg.sender)){
      sellerAddr = _sellerAddr;
      sellerContract = Seller(sellerAddr);
      producerAddr = _producerAddr;
      producerContract = Producer(producerAddr);
      platformSideAddr = _platformSideAddr;
      platformSideContract = PlatformSide(platformSideAddr);
    }
  }

  //
  function setSellerAddr(string _sellerID,address _sellerAddr)public{
    if(isMaster(msg.sender)){
      sellerContract.setSellerAddress(_sellerID,_sellerAddr);
    }
  }
  function setProducerAddr(string _producerID,address _producerAddr)public{
    if(isMaster(msg.sender)){
      producerContract.setProducerAddr(_producerID,_producerAddr);
    }
  }

  //
  function setSellerInfo(string _clothID,string _data,uint256 _confirmNum,uint256 _estimateTime) public{
      if(isMaster(msg.sender)){
        sellerContract.setSellerData(_clothID,_data);
        producerContract.setEstimateTime(_clothID,_estimateTime);
        producerContract.setConfirmNumber(_clothID,_confirmNum);
      }
  }

  function setOrderInfo(string _oriderID,string _dataInfo,uint256 _shipTimes) public {
    if(isMaster(msg.sender)){
        producerContract.setOrderData(_oriderID,_dataInfo);
        producerContract.setShipTime(_oriderID,_shipTimes);
    }
  }

  function checkConfirmNumber(string _clothID,uint256 _confirmNumBest)public view returns(bool){
      bool sign = false;
      uint256 confirmData =  producerContract.getConfirmNumber(_clothID);
      if(confirmData == _confirmNumBest){
        sign = true;
      }
      return sign;
  }

  function computerIssue(uint256 clothPrice,uint256 clothsols)public view returns(uint256){
    return producerContract.computerIssue(clothPrice,clothsols);
  }
  




}
