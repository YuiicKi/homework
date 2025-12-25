<template>
  <div class="certificate-wrapper">
    <div id="certificate-box" class="certificate-box">
      <div class="border-pattern"></div>
      
      <div class="content">
        <div class="header">
          <img src="/vite.svg" class="logo" alt="Logo" />
          <h1 class="title">荣誉证书</h1>
          <h2 class="subtitle">CERTIFICATE OF ACHIEVEMENT</h2>
        </div>

        <div class="body">
          <p class="text">特此证明 / This is to certify that</p>
          <h2 class="student-name">{{ studentName }}</h2>
          
          <p class="text">
            在 <strong>{{ examName }}</strong> 考试中成绩优异，通过考核。
            <br/>
            Has successfully passed the examination of <strong>{{ examName }}</strong>.
          </p>

          <div class="score-section">
            <span>成绩 / Score：</span>
            <span class="score-value">{{ score }}</span>
          </div>
        </div>

        <div class="footer">
          <div class="left">
            <p>证书编号 / No.</p>
            <p class="code">{{ certificateNumber }}</p>
          </div>
          <div class="right">
            <p>发证日期 / Date</p>
            <p class="date">{{ date }}</p>
            <div class="signature-line">教务处 (Academic Affairs)</div>
          </div>
        </div>
        
        <div class="stamp">
          <div class="stamp-inner">考试专用章</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  studentName: string
  examName: string
  score: number | string
  certificateNumber: string
  date: string
}>()
</script>

<style scoped>
/* 关键：这个容器平时需要隐藏，或者在屏幕外，
   但为了 html2canvas 能截图，不能 display: none。
   我们在父组件控制它的渲染位置。
*/
.certificate-wrapper {
  width: 1123px; /* A4 297mm @ 96dpi approx */
  height: 794px; /* A4 210mm @ 96dpi approx */
  background: #fff;
  color: #333;
  font-family: "Times New Roman", "SimSun", serif;
  box-sizing: border-box;
}

.certificate-box {
  width: 100%;
  height: 100%;
  padding: 40px;
  position: relative;
  border: 10px solid #8c7353; /* 金色边框 */
  background: #fffcf5; /* 象牙白底色 */
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.border-pattern {
  position: absolute;
  top: 5px; left: 5px; right: 5px; bottom: 5px;
  border: 2px solid #8c7353;
  pointer-events: none;
}

.header { text-align: center; margin-bottom: 40px; }
.logo { height: 60px; margin-bottom: 10px; }
.title { font-size: 48px; margin: 0; color: #8c7353; letter-spacing: 5px; }
.subtitle { font-size: 18px; margin: 10px 0 0; color: #555; text-transform: uppercase; }

.body { text-align: center; flex: 1; display: flex; flex-direction: column; justify-content: center; }
.text { font-size: 20px; line-height: 1.8; color: #555; margin: 20px 0; }
.student-name { font-size: 42px; font-weight: bold; margin: 10px 0; text-decoration: underline; text-underline-offset: 8px; font-family: "KaiTi", cursive; }
.score-section { margin-top: 30px; font-size: 24px; color: #d9534f; font-weight: bold; }
.score-value { font-size: 36px; }

.footer { display: flex; justify-content: space-between; margin-top: 60px; padding: 0 60px; }
.footer p { margin: 5px 0; font-size: 16px; color: #666; }
.footer .code { font-family: monospace; font-size: 18px; font-weight: bold; color: #333; }
.footer .date { font-size: 18px; font-weight: bold; border-bottom: 1px solid #333; display: inline-block; min-width: 150px; text-align: center; }
.signature-line { margin-top: 20px; border-top: 1px solid #333; padding-top: 10px; width: 200px; text-align: center; font-weight: bold; }

.stamp {
  position: absolute;
  bottom: 80px;
  right: 120px;
  width: 140px;
  height: 140px;
  border: 4px solid #d9534f;
  border-radius: 50%;
  color: #d9534f;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: rotate(-20deg);
  opacity: 0.8;
  mask-image: radial-gradient(circle, rgba(0,0,0,1) 60%, rgba(0,0,0,0.5) 100%); /* 简单模拟印章纹理 */
}
.stamp-inner {
  font-size: 18px;
  font-weight: bold;
  border: 1px solid #d9534f;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-family: "KaiTi";
}
</style>