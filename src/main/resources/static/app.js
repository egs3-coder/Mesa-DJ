const $=s=>document.querySelector(s); const tracksEl=$("#tracks");
let state={tracks:[],playing:false,soloId:null}; let timer=null; let busy=false;
async function api(action,id){ if(busy)return; busy=true; try{const u=new URL('/api/control',location.origin);u.searchParams.set('action',action);if(id)u.searchParams.set('id',id);const r=await fetch(u,{method:'POST'}); if(!r.ok)throw Error(); state=await r.json(); render();}catch(e){setConnection(false)}finally{busy=false}}
async function poll(){try{const r=await fetch('/api/state',{cache:'no-store'});if(!r.ok)throw Error();state=await r.json();setConnection(true);render()}catch(e){setConnection(false)}}
function setConnection(ok){$('#dot').classList.toggle('ok',ok);$('#connection').textContent=ok?'Motor Java conectado':'Motor Java indisponível'}
function render(){
 const tracks=state.tracks||[]; if(!tracks.length)return;
 const master=tracks.find(t=>t.principal)||tracks[0]; const pos=(master.positionMicros||0)/1e6, dur=(master.durationMicros||0)/1e6;
 $('#time').textContent=`${fmt(pos)} / ${fmt(dur)}`; $('#progressFill').style.width=dur?`${Math.min(100,pos/dur*100)}%`:'0%'; $('#progressGlow').style.left=dur?`${Math.min(100,pos/dur*100)}%`:'0%'; $('#masterState').textContent=master.state;
 $('#play').innerHTML=state.playing?'❚❚ <span>Pause</span>':'▶ <span>Play</span>'; $('#play').classList.toggle('playing',state.playing);
 tracksEl.innerHTML=''; tracks.forEach(t=>{const card=document.createElement('article');card.className='track';card.dataset.id=t.id; if(t.principal)card.classList.add('master-track');if(t.state==='PLAYING')card.classList.add('running');if(t.muted)card.classList.add('muted');if(t.soloed)card.classList.add('soloed');
 const pct=t.durationMicros?Math.min(100,(t.positionMicros/t.durationMicros)*100):0;
 card.innerHTML=`<div class="track-top"><span class="num">${String(tracks.indexOf(t)+1).padStart(2,'0')}</span><span class="status">${t.state}</span></div><div class="name">${esc(t.name)}</div><div class="thread"><div class="pulse" style="--p:${pct}%"></div><div class="thread-line"><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div><div class="position">${fmt((t.positionMicros||0)/1e6)}</div></div><div class="actions"><button class="mute ${t.muted?'on':''}">${t.muted?'Unmute':'Mute'}</button><button class="solo ${t.soloed?'on':''}">${t.soloed?'Unsolo':'Solo'}</button></div>`;
 card.querySelector('.mute').onclick=()=>api('mute',t.id); card.querySelector('.solo').onclick=()=>api('solo',t.id); tracksEl.appendChild(card);
 }); }
function fmt(x){x=Math.max(0,Math.floor(Number(x)||0));return `${String(Math.floor(x/60)).padStart(2,'0')}:${String(x%60).padStart(2,'0')}`}
function esc(s){return String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
$('#play').onclick=()=>api(state.playing?'pause':'play'); $('#stop').onclick=()=>api('stop'); $('#masterVolume').oninput=e=>$('#masterValue').textContent=e.target.value+'%';
$('#progressFill').parentElement.onclick=e=>{const rect=e.currentTarget.getBoundingClientRect(); const ratio=Math.max(0,Math.min(1,(e.clientX-rect.left)/rect.width)); const dur=(state.durationMicros||0)/1e6; if(dur>0) seek(ratio*dur)};
async function seek(seconds){try{const u=new URL('/api/seek',location.origin);u.searchParams.set('seconds',seconds);const r=await fetch(u,{method:'POST'});if(!r.ok)throw Error();state=await r.json();render()}catch(e){setConnection(false)}}
setInterval(poll,250); poll();
