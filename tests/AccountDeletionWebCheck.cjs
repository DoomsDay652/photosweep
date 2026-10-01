const test=require('node:test'), assert=require('node:assert/strict'), vm=require('node:vm'), fs=require('node:fs');
const code=fs.readFileSync('store/public/delete-account.js','utf8');
async function fixture({confirm=true,authOk=true,configured=true}={}){
 const nodes=Object.fromEntries(['form','status','email','password','confirm','delete','reset'].map(id=>[id,{value:'',disabled:false,handlers:{},addEventListener(name,handler){this.handlers[name]=handler;},reportValidity(){return true;},reset(){nodes.email.value='';nodes.password.value='';}}]));
 const calls=[];nodes.email.value='tester@photosweep.test';nodes.password.value='not-a-real-password';
 vm.runInNewContext(code,{document:{getElementById:id=>nodes[id]},window:{confirm:()=>confirm},fetch:async(url,options)=>{
  if(url==='account-config.json')return {ok:configured,json:async()=>({apiKey:'test-key'})};
  calls.push({url,body:JSON.parse(options.body)});
  return {ok:!url.includes('signInWithPassword')||authOk,json:async()=>url.includes('signInWithPassword')?{idToken:'verified-token'}:{}};
 },encodeURIComponent});
 await new Promise(resolve=>setImmediate(resolve));
 return {nodes,calls,submit:()=>nodes.form.handlers.submit({preventDefault(){}})};
}
test('Deletion authenticates first and sends only verified token to delete',async()=>{
 const f=await fixture();await f.submit();assert.equal(f.calls.length,2);assert.match(f.calls[0].url,/signInWithPassword/);assert.match(f.calls[1].url,/accounts:delete/);
 assert.deepEqual(f.calls[1].body,{idToken:'verified-token'});assert.equal(f.nodes.password.value,'');assert.match(f.nodes.status.textContent,/has been deleted/);
});
test('Wrong password never deletes an account or reports success',async()=>{
 const f=await fixture({authOk:false});await f.submit();assert.equal(f.calls.length,1);assert.equal(f.nodes.password.value,'');assert.doesNotMatch(f.nodes.status.textContent,/has been deleted/);
});
test('Cancel and missing configuration send no credentials',async()=>{
 for(const options of [{confirm:false},{configured:false}]){const f=await fixture(options);await f.submit();assert.equal(f.calls.length,0);}
});
