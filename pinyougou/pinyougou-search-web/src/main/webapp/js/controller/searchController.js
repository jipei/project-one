app.controller("searchController", function ($scope,$location, searchService) {

    //搜索条件对象
    $scope.searchMap = {"keywords":"", "category":"", "brand":"", "spec":{}, "price":"", "pageNo":1, "pageSize":20, "sortField":"", "sort":""};

    //搜索
    $scope.search = function () {
        searchService.search($scope.searchMap).success(function (response) {
            $scope.resultMap = response;

            //构建分页导航条
            buildPageInfo();
        });

    };

    //添加过滤条件
    $scope.addSearchItem = function (key, value) {
        if ("category" == key || "brand" == key || "price" == key) {
            $scope.searchMap[key] = value;
        } else {
            //规格
            $scope.searchMap.spec[key] = value;
        }
        $scope.searchMap.pageNo = 1;

        //重新搜索
        $scope.search();

    };

    //移除过滤条件
    $scope.removeSearchItem = function (key) {
        if ("category" == key || "brand" == key || "price" == key) {
            $scope.searchMap[key] = "";
        } else {
            //规格
            delete $scope.searchMap.spec[key];
        }
        $scope.searchMap.pageNo = 1;
        //重新搜索
        $scope.search();

    };

    //构建分页导航条方法
    buildPageInfo = function () {
        //页面中要显示的页号数组
        $scope.pageNoList= [];
        //在导航条中总显示页号数
        var showPageNoTotal = 5;
        //起始页号
        var startPageNo = 1;
        //结束页号
        var endPageNo = $scope.resultMap.totalPages;

        //如果总页数大于要显示的页号数
        if($scope.resultMap.totalPages > showPageNoTotal) {

            //当前页号的左右间隔
            var interval = Math.floor(showPageNoTotal/2);

            startPageNo = parseInt($scope.searchMap.pageNo) - interval;
            endPageNo = parseInt($scope.searchMap.pageNo) + interval;

            if(startPageNo >= 1) {
                // 如果结束页号是大于总页数的则都设置为总页数，起始页号就为要总页数-要显示的页数并加1
                if(endPageNo > $scope.resultMap.totalPages){
                    startPageNo = $scope.resultMap.totalPages - showPageNoTotal + 1;
                    endPageNo = $scope.resultMap.totalPages;
                }
            } else {
                // 如果起始页号是小于1的则都设置为1，结束页号就为要显示的总页号数
                startPageNo = 1;
                endPageNo = showPageNoTotal;
            }

        }

        // 前面3个点：如果起始页号大于1则存在；
        $scope.frontDot = false;
        if (startPageNo > 1) {
            $scope.frontDot = true;
        }
        // 后面3个点：如果结束页号小于总页数则存在；
        $scope.backDot = false;
        if (endPageNo < $scope.resultMap.totalPages) {
            $scope.backDot = true;
        }

        //获得正确的页号
        for (var i = startPageNo; i <= endPageNo; i++) {
            $scope.pageNoList.push(i);
        }


    };

    //判断是否当前页号
    $scope.isCurrentPage = function (pageNo) {
        return $scope.searchMap.pageNo==pageNo;
    };

    //跳转到某个页
    $scope.queryByPageNo = function (pageNo) {
        pageNo = parseInt(pageNo);
        if(pageNo > 0 && pageNo <= $scope.resultMap.totalPages){
            $scope.searchMap.pageNo = pageNo;
            $scope.search();
        }

    };

    //设置排序
    $scope.sortSearch = function (sortField, sort) {
        $scope.searchMap.sortField = sortField;
        $scope.searchMap.sort = sort;

        $scope.search();
    };

    //加载搜索关键字并搜索
    $scope.loadKeywords = function () {
        //获取地址栏中携带的搜索关键字
        $scope.searchMap.keywords = $location.search()["keywords"];
        $scope.search();
    };

});