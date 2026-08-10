<script setup lang="ts">
const router = useRouter()
</script>

<template>
  <div class="error">
    <div class="page-404">
      <h1>404</h1>
      <p>You are lost this page</p>
      <p class="info">
        you've reached the edge of universe.The Page you've requested , can't befound. Don't Worry ,
        you can return to previous page.
      </p>
      <button class="go-home btn" @click="router.push('/')">GO HOME</button>
      <button class="back btn" @click="router.back()">BACK</button>
    </div>
    <div class="astronaut">
      <img src="~/assets/img/404/astronaut.png" class="astronaut-img" alt="" />
    </div>
  </div>
</template>

<style>
@font-face {
  font-family: dogica;
  src: url(~/assets/font/dogica.ttf);
}

.error {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: url(~/assets/img/404/bg.png);
  background-attachment: fixed;
  background-size: cover;
}
.error .astronaut {
  transform: rotate(0deg) translate(0, -18%);
  animation-name: PAnimation;
  animation-direction: alternate;
  animation-timing-function: linear;
  animation-iteration-count: infinite;
  animation-duration: 3s;
}
.error .page-404 {
  width: 40vw;
  font-family: dogica;
  z-index: 1;
  color: #000000;
}
.error .page-404 h1 {
  font-size: 5vw;
  margin-bottom: 30px;
}
.error .page-404 p {
  font-size: 1vw;
  margin: 10px 0;
  line-height: 2vw;
}
.error .page-404 .btn {
  width: 10vw;
  height: 50px;
  border-radius: 30px;
  margin-top: 10px;
  cursor: pointer;
  transition: 0.3s;
  background-color: #fff;
  color: #000;
  border: 2px solid #000;
  font-size: 1vw;
}
.error .page-404 .go-home {
  margin-right: 15px;
}
.error .page-404 .btn:hover {
  background-color: #000;
  color: #fff;
}

@media (min-width: 1024px) {
  .error {
    position: relative;
    display: flex;
  }
  .error .astronaut {
    position: absolute;
    left: 50%;
    top: 50%;
  }
  .error .page-404 {
    width: 40vw;
    position: absolute;
    left: 10%;
    top: 40%;
    transform: translateY(-50%);
  }
}
@media (max-width: 1024px) {
  .error {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
  }
  .error .page-404 {
    width: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    margin-top: 10vh;
  }
  .error .page-404 h1 {
    font-size: 10vw;
    margin-bottom: 30px;
  }
  .error .page-404 p {
    font-size: 3vw;
    line-height: 6vw;
  }
  .error .page-404 .info {
    display: none;
  }
  .error .page-404 .btn {
    width: 30vw;
  }
  .astronaut {
    width: 100%;
    display: flex;
    justify-content: center;
  }
  .astronaut .astronaut-img {
    width: 70%;
    height: 80%;
    transform: translateY(40%);
  }
}
@keyframes PAnimation {
  0% {
    transform: rotate(0deg) translate(0, -15%);
  }
  100% {
    transform: rotate(10deg) translate(0, -18%);
  }
}
</style>
