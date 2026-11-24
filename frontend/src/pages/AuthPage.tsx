import { FormEvent, ReactNode, useEffect, useMemo, useState, type CSSProperties } from "react";
import type { Page, IconName } from "../types";
import { Icon } from "../components/Icon";
import { navigate } from "../lib/nav";
import { Logo, Button, Field } from "../components/ui";
import { authApi } from "../api/auth";
import { ApiError } from "../api/client";
import { useAuth } from "../context/AuthContext";

export function AuthPage({register}:{register:boolean}) {
  const { login } = useAuth();
  const [f,setF]=useState({firstName:"",lastName:"",email:"",phone:"",country:"Uganda",age:"",password:"",confirm:""});
  const set=(k:keyof typeof f)=>(v:string)=>setF(p=>({...p,[k]:v}));
  const [error,setError]=useState(""); const [notice,setNotice]=useState(""); const [busy,setBusy]=useState(false);
  async function submit(e:FormEvent){
    e.preventDefault();setError("");setNotice("");
    if(register&&f.password!==f.confirm){setError("Passwords do not match.");return}
    setBusy(true);
    try{
      if(register){
        const r=await authApi.register({firstName:f.firstName,lastName:f.lastName,email:f.email,phone:f.phone,country:f.country,age:Number(f.age),password:f.password});
        setNotice(r.message);
      } else { await login(f.email,f.password); navigate("dashboard"); }
    }catch(err){ setError(err instanceof ApiError?err.message:"Could not reach the server. Check your connection."); }
    finally{setBusy(false)}
  }
  return <div className="min-h-screen bg-[#f6f5f2]"><div className="mx-auto flex min-h-screen max-w-[1200px] items-stretch"><div className="hidden w-[42%] flex-col justify-between bg-[#1a1a1a] p-10 text-white lg:flex"><Logo light/><div><p className="text-sm font-bold uppercase tracking-[.17em] text-[#dcc690]">CONNECT · LEARN · CREATE</p><h1 className="mt-4 text-4xl font-extrabold leading-tight">{register?"Your journey starts with one step.":"Welcome back, changemaker."}</h1><p className="mt-4 leading-7 text-white/65">{register?"Join thousands of young people building skills and solutions across East Africa.":"Continue learning, collaborating, and turning your ideas into impact."}</p></div><p className="text-xs text-white/35">Youth Empowerment Project · East Africa</p></div>
    <div className="flex flex-1 items-center justify-center p-5 sm:p-10"><div className="w-full max-w-xl"><button onClick={()=>navigate("home")} className="mb-8 text-sm font-bold text-[#1a1a1a]">← Back to home</button><p className="eyebrow">{register?"CREATE YOUR ACCOUNT":"WELCOME BACK"}</p><h1 className="mt-2 text-3xl font-extrabold text-[#1a1a1a]">{register?"Join the YEP community":"Log in to YEP"}</h1><p className="mt-2 text-sm text-slate-500">{register?"Free to join. Designed for young people aged 18–35.":"Log in with the email and password you registered with."}</p>
      <form onSubmit={submit} className="mt-7 space-y-4">{register&&<div className="grid gap-4 sm:grid-cols-2"><Field label="First name" placeholder="Amina" value={f.firstName} onChange={set("firstName")} autoComplete="given-name"/><Field label="Last name" placeholder="Kato" value={f.lastName} onChange={set("lastName")} autoComplete="family-name"/></div>}<Field label="Email address" type="email" placeholder="amina@example.com" value={f.email} onChange={set("email")} autoComplete="email"/>{register&&<><div className="grid gap-4 sm:grid-cols-2"><Field label="Phone number" type="tel" placeholder="+256 700 000 000" value={f.phone} onChange={set("phone")} autoComplete="tel"/><label className="field-label">Country<select required value={f.country} onChange={e=>set("country")(e.target.value)} className="form-input"><option>Uganda</option><option>Kenya</option><option>Tanzania</option><option>Rwanda</option><option>Burundi</option></select></label></div><div className="grid gap-4 sm:grid-cols-2"><Field label="Age" type="number" placeholder="24" value={f.age} onChange={set("age")}/></div></>}<div className="grid gap-4 sm:grid-cols-2"><Field label="Password" type="password" placeholder="At least 8 characters" value={f.password} onChange={set("password")} autoComplete={register?"new-password":"current-password"}/>{register&&<Field label="Confirm password" type="password" placeholder="Repeat password" value={f.confirm} onChange={set("confirm")} autoComplete="new-password"/>}</div>{register&&<label className="flex gap-3 text-xs leading-5 text-slate-500"><input type="checkbox" required className="mt-1 accent-[#1a1a1a]"/>I agree to the community guidelines and consent to the secure processing of my registration data.</label>}{error&&<p role="alert" className="text-sm text-red-600">{error}</p>}{notice&&<p role="status" className="rounded-xl border border-[#efece6] bg-[#efece6] p-3 text-sm font-semibold text-[#8a6d2f]">{notice}</p>}{!register&&<button type="button" onClick={()=>navigate("forgot")} className="block text-sm font-bold text-[#1a1a1a]">Forgot your password?</button>}
        <Button type="submit" variant="secondary" className="w-full">{busy?"Please wait…":register?"Create my account":"Log in securely"} <Icon name="arrow" size={16}/></Button></form>
      <p className="mt-6 text-center text-sm text-slate-500">{register?"Already a member?":"New to YEP?"} <button onClick={()=>navigate(register?"login":"register")} className="font-extrabold text-[#1a1a1a]">{register?"Log in":"Create an account"}</button></p>
    </div></div></div></div>;
}

