pragma solidity ^0.4.22;
library SafeMath {
      function mul(uint256 a, uint256 b) internal pure returns (uint256) {
        if (a == 0) {
          return 0;
        }
        uint256 c = a * b;
        assert(c / a == b);
        return c;
      }

      function div(uint256 a, uint256 b) internal pure returns (uint256) {
        // assert(b > 0); // Solidity automatically throws when dividing by 0
        uint256 c = a / b;
        // assert(a == b * c + a % b); // There is no case in which this doesn't hold
        return c;
      }

      function sub(uint256 a, uint256 b) internal pure returns (uint256) {
        assert(b <= a);
        return a - b;
      }

      function add(uint256 a, uint256 b) internal pure returns (uint256) {
        uint256 c = a + b;
        assert(c >= a);
        return c;
      }
    }

contract Seller{

  using SafeMath for uint256;

  mapping(string=>string) sellerDataBase;
  mapping(string=>address) sellerAddress;

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


  function getSellerData(string clothIDAndSellerID)public view returns(string){
    return sellerDataBase[clothIDAndSellerID];
  }

  function setSellerAddress(string sellerID,address sellerAddr) public {
    if(isMaster(msg.sender)){
      sellerAddress[sellerID] = sellerAddr;
    }
  }
  function getSellerAddress(string sellerID) public view returns(address){
      return sellerAddress[sellerID];
  }
  function setSellerData(string clothIDAndSellerID,string clothData) public {
    if(isMaster(msg.sender)){
      sellerDataBase[clothIDAndSellerID] = clothData;
    }
  }
  function computerIssue(uint256 clothPrice,uint256 clothsols)public view returns(uint256){
    return clothPrice.mul(clothsols);
  }


}
