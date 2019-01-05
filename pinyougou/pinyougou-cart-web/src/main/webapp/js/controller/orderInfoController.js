app.controller("orderInfoController", function ($scope, cartService, addressService) {


    $scope.getUsername = function () {
        cartService.getUsername().success(function (response) {
            $scope.username = response.username;
        });
    };

    //获取购物车列表
    $scope.findCartList = function () {
        cartService.findCartList().success(function (response) {
            $scope.cartList = response;

            //计算商品的总价和总数量
            $scope.totalValue = cartService.sumTotalValue(response);
        });
    };

    //获取当前登陆用户的收件人地址列表
    $scope.findAddressList = function () {
        addressService.findAddressList().success(function (response) {
            $scope.addressList = response;

            //查询默认地址
            for (var i = 0; i < response.length; i++) {
                var address = response[i];
                if ("1" == address.isDefault) {
                    $scope.address = address;
                    break;
                }
            }

        });
    };

    //选择地址
    $scope.selectAddress = function (address) {
        $scope.address = address;
    };

    //判断当前地址是否是选择的地址
    $scope.isSelectedAddress = function (address) {
        return $scope.address == address;
    };

    //支付方式；默认为微信付款
    $scope.order = {"paymentType": "1"};

    //选择支付方式
    $scope.selectPaymentType = function (type) {
        $scope.order.paymentType = type;
    };

    //提交订单
    $scope.submitOrder = function () {
        //设置订单的信息
        $scope.order.receiverAreaName = $scope.address.address;
        $scope.order.receiverMobile = $scope.address.mobile;
        $scope.order.receiver = $scope.address.contact;

        cartService.submitOrder($scope.order).success(function (response) {
            if (response.success) {
                if ("1" == $scope.order.paymentType) {
                    //微信付款；跳转到扫描页面
                    location.href = "pay.html#?outTradeNo=" + response.message;
                } else {
                    //货到付款;跳转到成功页面
                    location.href = "paysuccess.html";
                }
            } else {
                alert(response.message);
            }
        });
    };
});