pragma solidity ^0.4.22;

contract Producer{


  mapping(string=>address) producerAddr;
  mapping(string=>string)  orderData;
  mapping(string=>uint256) confirmNumber;
  mapping(string=>uint256) estimateTime;
  mapping(string=>uint256) shipTime;

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
  function  setEstimateTime(string _clothID,uint256 _estimateTime) public{
    if(isMaster(msg.sender)){
      estimateTime[_clothID] = _estimateTime;
    }
  }
  function  getEstimateTime(string _clothID) public view returns(uint256){
    return estimateTime[_clothID];
  }

  function setShipTime(string _orderID,uint256 _shipTime) public{
    if(isMaster(msg.sender)){
      shipTime[_orderID] = _shipTime;
    }
  }

  function  getShipTime(string _orderID) public view returns(uint256){
    return shipTime[_orderID];
  }

  function setOrderData(string _orderID,string _dataBase)public{
    if(isMaster(msg.sender)){
      orderData[_orderID] = _dataBase;
    }
  }
  function getOrderData(string _orderID) public view returns(string){
    return orderData[_orderID];
  }
  function setProducerAddr(string _producerID,address _producerAddr) public{
    if(isMaster(msg.sender)){
      producerAddr[_producerID] = _producerAddr;
    }
  }
  function getProducerAddr(string _producerID) public view returns(address){
    return producerAddr[_producerID];
  }
  function setConfirmNumber(string clothID,uint256 confirmNum) public{
    if(isMaster(msg.sender)){
      confirmNumber[clothID] = confirmNum;
    }
  }
  function getConfirmNumber(string clothID)public view returns(uint256){
    return confirmNumber[clothID];
  }


}
