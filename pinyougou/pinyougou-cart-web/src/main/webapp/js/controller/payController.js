app.controller("payController",function ($scope,$location,cartService,payService) {
    $scope.getUsername = function () {
        cartService.getUsername().success(function (response) {
            $scope.username = response.username;
        });
    };

    //生成二维码
    $scope.createNative=function () {
        //1.接受浏览器地址栏的订单号

        $scope.outTradeNo=$location.search()["outTradeNo"];
        //2.发送请求到后台
        payService.createNative($scope.outTradeNo).success(function (response) {

            if ("SUCCESS"==response.result_code){
                //总金额
                $scope.totalFee=(response.totalFee/100).toFixed(2);

                //3.生成二维码
                var qr=new QRious({
                    element:document.getElementById("qrious"),
                    size:250,
                    level:"Q",
                    value:response.code_url
                });

                //查询支付状态
                queryPayStatus($scope.outTradeNo);
            } else {
                alert("生成二维码失败");
            }
        });
    };

    //查询支付状态
    queryPayStatus=function (outTradeNo) {

        payService.queryPayStatus(outTradeNo).success(function (response) {
            if (response.success) {
                //跳转到成功页面
                location.href="paysuccess.html#?money="+$scope.totalFee;
            }else {
                if ("支付超时" == response.message) {
                    alert(response.message)
                    //重新生成二维码
                    $scope.createNative();
                }else {
                    //跳转到失败页面
                    location.href="payfail.html";
                }

            }
        });
    };

    //获取地址栏中的金额
    $scope.getMoney=function () {

        $scope.money=$location.search()["money"];
    };
});