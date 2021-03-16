/**
 * 获取当前时间
 */
const getNow = function() {
  let date = new Date();
  let year = date.getFullYear();
  let month =  date.getMonth() < 9 ? "0"+ (date.getMonth()+1) : (date.getMonth()+1);
  let day = date.getDate() < 10 ? "0"+date.getDate() : date.getDate();
  let hours = date.getHours() < 10 ? "0"+date.getHours() : date.getHours();
  let minutes = date.getMinutes() < 10 ? "0"+date.getMinutes() : date.getMinutes();
  let seconds = date.getSeconds() < 10 ? '0' + date.getSeconds() : date.getSeconds();
  let now = year + "-" + month + "-" + day  + " " + hours + ":" + minutes + ':' + seconds;
  return now
};
export { getNow }

 /**
  * 完整计算时间差(天、小时、分钟、秒)
  */
const getTimeDifference = function(d1) {
  //如果时间格式是正确的，那下面这一步转化时间格式就可以不用了
  let dateBegin = new Date(d1.replace(/-/g, "/"));//将-转化为/，使用new Date
  let dateEnd = new Date();//获取当前时间
  let dateDiff = dateEnd.getTime() - dateBegin.getTime();//时间差的毫秒数
  let dayDiff = Math.floor(dateDiff / (24 * 3600 * 1000));//计算出相差天数
  let leave1=dateDiff%(24*3600*1000) //计算天数后剩余的毫秒数
  let hours=Math.floor(leave1/(3600*1000))//计算出小时数
  //计算相差分钟数
  let leave2=leave1%(3600*1000) //计算小时数后剩余的毫秒数
  let minutes=Math.floor(leave2/(60*1000))//计算相差分钟数
  //计算相差秒数
  let leave3=leave2%(60*1000) //计算分钟数后剩余的毫秒数
  let seconds=Math.round(leave3/1000)
  return dayDiff + "天" + hours + "小时" + minutes + "分钟" + seconds + "秒"
};
export { getTimeDifference }

const scrollAnimation = function (currentY, targetY) {
// 获取当前位置方法
    // const currentY = document.documentElement.scrollTop || document.body.scrollTop
    // 计算需要移动的距离
    let needScrollTop = targetY - currentY;
    let _currentY = currentY;
    setTimeout(() => {
        // 一次调用滑动帧数，每次调用会不一样
        const dist = Math.ceil(needScrollTop / 10);
        _currentY += dist;
        window.scrollTo(_currentY, currentY);
        // 如果移动幅度小于十个像素，直接移动，否则递归调用，实现动画效果
        if (needScrollTop > 10 || needScrollTop < -10) {
            scrollAnimation(_currentY, targetY)
        } else {
            window.scrollTo(_currentY, targetY)
        }
    }, 10)
};
export { scrollAnimation }
