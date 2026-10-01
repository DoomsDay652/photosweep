'use strict';
let config;
const form=document.getElementById('form'),status=document.getElementById('status');
const email=document.getElementById('email'),password=document.getElementById('password');
const buttons=[document.getElementById('delete'),document.getElementById('reset')];
const lock=value=>buttons.forEach(button=>button.disabled=value);
lock(true);
fetch('account-config.json',{cache:'no-store'}).then(r=>{if(!r.ok)throw Error();return r.json();}).then(c=>{
 if(!c.apiKey||c.apiKey.includes('REPLACE'))throw Error();config=c;status.textContent='Enter your account email and password to confirm ownership.';lock(false);
}).catch(()=>{status.textContent='Account deletion is not configured on this website. Please contact the developer through the Play Store listing.';});
async function request(operation,payload){
 const response=await fetch('https://identitytoolkit.googleapis.com/v1/accounts:'+operation+'?key='+encodeURIComponent(config.apiKey),{
  method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload),credentials:'omit',cache:'no-store'
 });
 if(!response.ok)throw Error('Could not complete this request. Check your connection and credentials or try again later.');
 return response.json();
}
form.addEventListener('submit',async event=>{
 event.preventDefault();if(buttons[0].disabled||!form.reportValidity())return;
 if(!window.confirm('Permanently delete this Photo Sweep account? This cannot be undone.'))return;
 lock(true);status.textContent='Verifying your account…';
 let token;
 try{
  const account=await request('signInWithPassword',{email:email.value.trim(),password:password.value,returnSecureToken:true});
  password.value='';token=account.idToken;
  await request('delete',{idToken:token});form.reset();status.textContent='Your Photo Sweep account has been deleted.';
 }catch(error){status.textContent=error.message;}finally{token=undefined;password.value='';lock(false);}
});
buttons[1].addEventListener('click',async()=>{
 if(buttons[1].disabled||!email.reportValidity())return;lock(true);
 try{await request('sendOobCode',{requestType:'PASSWORD_RESET',email:email.value.trim()});status.textContent='If an account exists, a reset email has been sent.';}
 catch(error){status.textContent=error.message;}finally{password.value='';lock(false);}
});
