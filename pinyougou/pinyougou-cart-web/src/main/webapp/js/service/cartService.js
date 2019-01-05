app.service("cartService",function ($http) {
    this.getUsername=function () {
        return $http.get("cart/getUsername.do?t="+Math.random());
    };

    this.findCartList=function () {
        return $http.get("cart/findCartList.do?t="+Math.random());
    };

    this.addItemToCartList=function (itemId, num) {
        return $http.get("cart/addItemToCartList.do?itemId="+itemId+"&num="+num+"&t="+Math.random());
    };

    this.sumTotalValue=function (cartList) {
        var totaValue={"totalNum":0,"totalMoney":0.0};
        for (var i = 0; i <cartList.length ; i++) {
            var cart=cartList[i];
            for (var j = 0; j <cart.orderItemList.length; j++) {
                var orderItem =cart.orderItemList[j];
                totaValue.totalNum+=orderItem.num;
                totaValue.totalMoney+=orderItem.totalFee;
            }
        }
        return totaValue;
    };

    //提交订单
    this.submitOrder=function (order) {
        return $http.post("order/add.do",order);
    }
});