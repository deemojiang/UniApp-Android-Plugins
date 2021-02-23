<template>
	<view class="container">
		<view class="c-top">
			<view class="c-top-left" @click="getGoodsInfoList">
				<image src="../../static/logo-title.png"></image><!-- @click="goToTestApi"-->
			</view>
			<view class="c-top-right" @click="wantToManage">
				<view class="text day">{{getDay}}</view>
				<view class="text time">{{getTime}}</view>
			</view>
		</view>
		<view class="c-center">
			<swiper class="screen-swiper square-dot" :indicator-dots="true" :circular="true" :current="curSwiper"
			 :autoplay="false" interval="5000" duration="500" @change="cardSwiper">
				<swiper-item v-for="(list,index) in cutGoosList" :key="index">
					<view class="good-list">
						<view class="good-item" v-for="(item,index2) in list" :key="index2" @click="chooseGood(item)">
							<view class="good-img">
								<image :src="item.pic"></image>
								<!-- <image :src="require('../../static/goods/'+item.img)"></image> -->
							</view>
							<view class="good-content">
								<view class="text-center good-name">{{item.title}}</view>
								<view v-if="item.stock > 0" class="text-center good-value bg-red">
									<text class="lg text-white cuIcon-cart margin-right-xs"></text>
									{{item.redeemPoints}}积分
								</view>
								<view v-else class="text-center bg-grey good-value">
									<text class="text-white">已售罄</text>
								</view>
							</view>
						</view>						
					</view>
				</swiper-item>
			</swiper>
		</view>
		<view class="c-bottom">
			<adInfo></adInfo>
		</view>
		<view class="cu-modal show" v-if="modalName == 'good-detail'">
			<view class="cu-dialog">
				<goodDetail :good="good" @closeGoodDetail="closeGoodDetail" @confirmPay="confirmPay"></goodDetail>
			</view>
		</view>
		<view class="cu-modal show" v-if="modalName == 'pay-detail'">
			<view class="cu-dialog">
				<payDetail :good="good" @outGoods="outGoods" @closePaypay="closePayDetail"></payDetail>
			</view>
		</view>
		<view class="cu-modal show" v-if="modalName == 'goToManage'">
			<view class="cu-dialog inputPassword">
				
				<view class="detail-top">
					<text class="text-blue text-bold">输入密码进入管理界面</text>
				</view>
				<view class="detail-center margin-top">
					<input type="password" v-model="password" />
				</view>
				<view class="detail-bottom">
					<button class="cu-btn bg-blue margin-right" @click="closeManage"><text class="cuIcon-back_android margin-right-xs"></text>返回</button>
					<button class="cu-btn bg-blue" @click="goToManage"><text class="cuIcon-pay margin-right-xs"></text>确认</button>
				</view>
			</view>
		</view>
	</view>
</template>


<script>
	import {post,get,showModal,IsNull,IsNullOrWhiteSpace,formatDate,JSONParse,CheckNewVersion} from '../../common/util.js';
	import goodDetail from "../../components/good-detail.vue";
	import payDetail from "../../components/pay-detail.vue";
	import adInfo from "../../components/ad.vue"
	const ADP = uni.requireNativePlugin('ADP-Module');
	export default {
		components:{goodDetail, payDetail, adInfo},
		data() {
			return {
				nowTime: new Date(),
				timeInterval: null,
				getListInterval: null,
				modalName: null,
				good: {},
				goodsList: [
					{name: '黑色笔记本', value: 50, type:1, img: 'food_0.png'},
					{name: '福临门稻花香5KG', value: 50, type:0, img: 'food_1.png'},
					{name: '油性笔一盒（12支）', value: 50, type:1, img: 'food_2.png'},
					{name: '农夫山泉矿泉水380ml', value: 50, type:1, img: 'food_3.png'},
					{name: '黑人牙膏一支', value: 50, type:1, img: 'food_4.png'},
					{name: '金龙鱼玉米油4L', value: 50, type:1, img: 'food_5.png'},
					{name: '黑色笔记本', value: 50, type:1, img: 'food_0.png'},
					{name: '福临门稻花香5KG', value: 50, type:0, img: 'food_1.png'},
					{name: '油性笔一盒（12支）', value: 50, type:1, img: 'food_2.png'},
					{name: '农夫山泉矿泉水380ml', value: 50, type:1, img: 'food_3.png'},
					{name: '黑人牙膏一支', value: 50, type:1, img: 'food_4.png'},
					{name: '金龙鱼玉米油4L', value: 50, type:1, img: 'food_5.png'},
					{name: '黑色笔记本', value: 50, type:1, img: 'food_0.png'},
					{name: '福临门稻花香5KG', value: 50, type:0, img: 'food_1.png'},
					{name: '油性笔一盒（12支）', value: 50, type:1, img: 'food_2.png'},
					{name: '农夫山泉矿泉水380ml', value: 50, type:1, img: 'food_3.png'},
					{name: '黑人牙膏一支', value: 50, type:1, img: 'food_4.png'},
					{name: '金龙鱼玉米油4L', value: 50, type:1, img: 'food_5.png'},
				],
				
				manageTimeout: null,
				manageClickTimes: 0,
				password: "",
				
				deptId: 0,
				curSwiper: '',
				
			}
		},
		computed:{
			getDay(){
				var that = this,time = that.nowTime;
				var year = time.getFullYear(),
					month = (time.getMonth()+1) > 9 ? time.getMonth() + 1 : '0' + (time.getMonth() + 1),
					day = time.getDate() > 9 ? time.getDate() : '0' + time.getDate();
				return year + '-' + month + '-' + day;
			},
			getTime(){
				var that = this,time = that.nowTime;
				var hours = time.getHours() > 9 ? time.getHours() : '0' + time.getHours(),
					minutes = time.getMinutes() > 9 ? time.getMinutes() : '0' + time.getMinutes(),
					seconds = time.getSeconds() > 9 ? time.getSeconds() : '0' + time.getSeconds();
				return hours + ':' + minutes + ':' + seconds;
			},
			cutGoosList(){
				var that = this;
				let newArr = [],arr = that.goodsList || [], len = 6;
				for (let i = 0; i < arr.length;) {
				  newArr.push(arr.slice(i, i += len));
				}
				return newArr;
			},
		},
		onLoad() {
			var that = this;
			uni.getStorage({
			    key: 'd',
			    success: function (res) {
			       that.deptId = res.data;
			    }
			});
			if(that.deptId == 0 ){
				that.deptId = 2;
				uni.setStorage({
					key: 'd',
					data: that.deptId
				})
			}
			ADP.hideBottomUIMenu();
		},
		mounted(){
			var that = this;
			// plus.navigator.hideSystemNavigation();
			that.getGoodsInfoList();
			// that.getAdInfo();
			
			that.timeInterval = setInterval(()=>{
				that.nowTime = new Date();
			},1000);
			that.getListInterval = setInterval(()=>{
				that.getGoodsInfoList();
			},10000);
			
			that.submitFirst();
			setTimeout(()=>{
				that.initSecond()
			},1000);
			setTimeout(()=>{
				that.initThird()
			},2000);
			setTimeout(()=>{
				that.initFourth()
			},3000);
			
			
			
			
		},
		destroyed(){
			var that = this;
			ADP.closeADP();
			clearInterval(that.timeInterval);
			clearInterval(that.getListInterval);
		},
		methods: {
			cardSwiper(e){
				console.log(e.detail.current);
			},
			async getGoodsInfoList(){
				console.log(3232);
				var that = this;
				try
				{
					var params = {
						deptId: that.deptId,
					};
					let r = await get(that.ty_app + 'wlxclxStoreCommodity/getGoodsInfoList',params );
					that.goodsList = r.result;
				}catch(e) {
					uni.showModal({title:"错误信息",content:e});
				}		
			},
			chooseGood(item){
				// 选择商品
				var that = this;
				if(item.stock >0){
					that.modalName = 'good-detail';
					that.good = item;					
				}
			},
			closeGoodDetail(){
				var that = this;
				that.modalName = null;
			},
			
			confirmPay(){
				var that = this;
				that.modalName = 'pay-detail';
			},
			closePayDetail(){
				var that = this;
				that.modalName = null;
			},
			goToTestApi(){
				uni.navigateTo({
					url: '../apiTest/apiTest',
				});
			},
			
			outGoods(aisle){
				var that = this;
				uni.showToast({
					title: "出货中，货道号为"+(aisle+1),
					icon:'none'
				});
				if(aisle>=60 && aisle <= 77){
					ADP.outGoodsSalve(aisle,result => {});
				}else if(aisle > 0 && aisle){
					ADP.outGoods(aisle,result => {});
				}
				that.getGoodsInfoList();  // 重新拉去列表
			},
			 
			submitFirst(){
				uni.showToast({
					title: "开始初始化...",
					icon:'none'
				});
				ADP.initADP('ttyS', 3, 38400, result => {
					
					uni.showToast({
						title: '初始化完成',
						icon: 'none'
					})
				});
			},
			initSecond(){
				uni.showToast({
					title: "启用副柜...",
					icon:'none'
				});
				// 初始化成功后 启用副柜
				ADP.enableSalve(true, false, result => {});
			},
			initThird(){
				
					uni.showToast({
						title: "启用升降机...",
						icon:'none'
					});
					// 启用副柜后 设置升降机使用
					ADP.enable812(true,5,result => {});
			},
			initFourth(){
				
				uni.showToast({
					title: "初始化升降机...",
					icon:'none'
				});
					// 初始化升降机
					ADP.run(result => {});
			},
			
			wantToManage(){
				var that = this;
				that.modalName = 'goToManage'
			},
			closeManage(){
				var that = this;
				that.modalName = null;
				that.password = '';
			},
			goToManage(){
				var that = this;
				if(that.password == 'admin1234'){
					that.modalName = null;
					uni.navigateTo({
						url: './manage',
					});
				}else{
					uni.showToast({
						title: '密码错误'
					})
				}
			}
		}
	}
</script>

<style lang="scss">
.container{
	width: 100vw;
	height: 100vh;
	overflow: hidden;
	background: #f6f7f7;
}

.c-top{
	width: 100vw;
	height: calc((154 / 1920) * 100vh);
	padding: 2vh 2vw;
	display: flex;
	justify-content: space-between;
	.c-top-left{
		height: 100%;
		image{
			height: 4vh;
			width: 70vw;
		}
	}
	.c-top-right{
		text-align: center;
		color: #0862bf;
		font-weight: bold;
		.day{
			font-size: calc((24 / 1080) * 100vw);
			line-height: 1.4;
		}
		.time{
			font-size: calc((40 / 1080) * 100vw);
			line-height: 1.4;
		}
	}
}

.c-center{
	width: 100vw;
	height: calc((1250 / 1920) * 100vh);
	margin-bottom: calc((12 / 1920) * 100vh);

	.good-list{
		height: calc(100% - 50px);
		display: flex;
		padding: 0 calc((25 / 1080) * 100vw);
		flex-wrap: wrap;
	}
}
.good-item:not(:nth-child(n+3)){
	margin-bottom: calc((50 / 1920) * 100vh);
}
.good-item:not(:nth-child(3n)){
	margin-right: calc((90 / 1080) * 100vw);
}
.good-img{
	width: calc((282 / 1080) * 100vw);
	height: calc((400 / 1920) * 100vh);
	margin-bottom: calc((18 / 1920) * 100vh);
	border-radius: 10upx;
	overflow: hidden;
	background: #fff;
	padding: 5upx;
	box-shadow: 5px 5px 10px rgba(0,0,0,0.15);
}
.good-img img{
	height: 100%;
}
.good-content{
	padding: calc((15 / 1080) * 100vw);
	background: #fff;
	border-radius: 10upx;
	overflow: hidden;
	box-shadow: 5px 5px 10px rgba(0,0,0,0.15);
}
.good-name{
	font-size: calc((23 / 1080) * 100vw);
}
.good-value{
	border-radius: 5upx;
	height: calc((46 / 1920) * 100vh);
	line-height: calc((46 / 1920) * 100vh);
	width: calc((230 / 1080) * 100vw);
	font-size: calc((30 / 1080) * 100vw);
	margin: calc((30 / 1920) * 100vh) auto 0;
}

.c-bottom{
	width: 100vw;
	height: calc((504 / 1920) * 100vh);
}
.c-bottom image{
	width: 100%;
	height: 100%;
}
.c-bottom video{
	width: 100%;
	height: 100%;
}

.screen-swiper{
	height: 100%;
}

.cu-dialog{
	width: calc((700 / 1080) * 100vw);
}



.inputPassword{
	padding: calc((30 / 1080) * 100vw);
	
	.detail-top{
		display: flex;
		justify-content: space-between;
		font-size: calc((36 / 1920) * 100vh);
	}
	.detail-center{
		height: calc((300 / 1920) * 100vh);
		display: flex;
		align-items: center;
		input{
			font-size: calc((30 / 1920) * 100vh);
			text-align: left;
			width: 100%;
			border-bottom: 1px solid #2C405A;
		}
	}
	.detail-bottom{
		display: flex;
		justify-content: center;
		button{
			padding: calc((30 / 1080) * 100vw);
			font-size: calc((24 / 1080) * 100vw);
		}
	}
}
</style>
