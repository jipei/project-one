app.controller("baseController", function ($scope) {
    //初始化分页导航条的配置
    $scope.paginationConf = {
        //页号
        currentPage:1,
        //页大小
        itemsPerPage:10,
        //总记录数
        totalItems:0,
        //每页页大小选择
        perPageOptions:[10,20,30,40,50],
        //改变页号之后加载事件
        onChange:function () {
            $scope.reloadList();
        }
    };

    $scope.reloadList = function(){
        //$scope.findPage($scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
        $scope.search($scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
    };

    //已选择了的那些id数组
    $scope.selectedIds = [];

    //复选框的点击事件
    $scope.updateSelection = function ($event, id) {
        if($event.target.checked){
            //选中
            $scope.selectedIds.push(id);
        } else {
            //反选则将id从数组中删除
            var index = $scope.selectedIds.indexOf(id);

            //删除指定位置的元素
            //参数1：位置
            //参数2：个数
            $scope.selectedIds.splice(index, 1);
        }

    };

});