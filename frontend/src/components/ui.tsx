import { FormEvent, ReactNode, useEffect, useId, useMemo, useState, type CSSProperties } from "react";
import type { Page, IconName } from "../types";
import { Icon } from "../components/Icon";
import { navigate } from "../lib/nav";

export function Logo({ light = false }: { light?: boolean }) {
  return (
    <button className="text-left leading-none" onClick={() => navigate("home")} aria-label="YEP home">
      <strong className={`block font-[Gilda_Display] text-[26px] font-normal tracking-[.32em] ${light ? "text-white" : "text-[#1a1a1a]"}`}>YEP</strong>
      <span className={`mt-1.5 block h-px w-full ${light ? "bg-[#c2a15a]" : "bg-[#c2a15a]"}`} />
      <span className={`mt-1.5 block text-[9px] tracking-[.3em] uppercase ${light ? "text-white/70" : "text-slate-500"}`}>Youth Empowerment</span>
    </button>
  );
}

export function Button({ children, onClick, variant = "primary", type = "button", className = "" }: { children: ReactNode; onClick?: () => void; variant?: "primary" | "secondary" | "ghost"; type?: "button" | "submit"; className?: string }) {
  const variants = {
    primary: "border border-[#1a1a1a] bg-[#1a1a1a] text-white hover:bg-[#c2a15a] hover:border-[#c2a15a] hover:text-[#1a1a1a]",
    secondary: "border border-[#1a1a1a] bg-transparent text-[#1a1a1a] hover:bg-[#1a1a1a] hover:text-white",
    ghost: "border border-slate-300 bg-transparent text-slate-700 hover:border-[#1a1a1a] hover:text-[#1a1a1a]",
  };
  return <button type={type} onClick={onClick} className={`inline-flex min-h-12 items-center justify-center gap-2 px-7 py-2.5 text-xs font-medium uppercase tracking-[.2em] transition-colors duration-200 ${variants[variant]} ${className}`}>{children}</button>;
}

export function PageHero({eyebrow,title,copy}:{eyebrow:string;title:string;copy:string}) {
  return <section className="border-b border-slate-200 bg-[#f6f5f2] py-20 sm:py-24"><div className="mx-auto max-w-[1200px] px-5 lg:px-8"><p className="eyebrow">{eyebrow}</p><h1 className="mt-4 max-w-3xl text-4xl leading-[1.1] sm:text-6xl">{title}</h1><span className="mt-7 block h-px w-16 bg-[#c2a15a]" /><p className="mt-6 max-w-xl text-lg font-light leading-8 text-slate-600">{copy}</p></div></section>;
}

export function PasswordInput({label,placeholder,value,onChange,name,autoComplete,required=true,minLength,className="form-input",labelClassName="block text-sm font-bold text-slate-700",hint}:{label:string;placeholder?:string;value?:string;onChange?:(v:string)=>void;name?:string;autoComplete?:string;required?:boolean;minLength?:number;className?:string;labelClassName?:string;hint?:string}) {
  const id = useId();
  const [visible, setVisible] = useState(false);
  return <div><label htmlFor={id} className={labelClassName}>{label}</label><div className="relative mt-1.5"><input id={id} required={required} name={name} autoComplete={autoComplete} type={visible?"text":"password"} minLength={minLength} className={`${className} pr-16`} placeholder={placeholder} value={value} onChange={onChange?e=>onChange(e.target.value):undefined}/><button type="button" aria-label={`${visible?"Hide":"Show"} password`} aria-pressed={visible} onClick={()=>setVisible(!visible)} className="absolute inset-y-0 right-0 px-2 text-xs font-bold normal-case tracking-normal text-slate-600 hover:text-[#8a6d2f] focus-visible:outline-offset-[-2px]">{visible?"Hide":"Show"}</button></div>{hint&&<span className="mt-1 block text-xs font-normal text-slate-400">{hint}</span>}</div>;
}

export function Field({label,type="text",placeholder,value,onChange,name,autoComplete}:{label:string;type?:string;placeholder:string;value?:string;onChange?:(v:string)=>void;name?:string;autoComplete?:string}) {
  if(type==="password") return <PasswordInput label={label} placeholder={placeholder} value={value} onChange={onChange} name={name} autoComplete={autoComplete} minLength={8} labelClassName="field-label"/>;
  return <label className="field-label">{label}<input required name={name} autoComplete={autoComplete} type={type} min={type==="number"?18:undefined} max={type==="number"?35:undefined} className="form-input" placeholder={placeholder} value={value} onChange={onChange?e=>onChange(e.target.value):undefined}/></label>;
}
