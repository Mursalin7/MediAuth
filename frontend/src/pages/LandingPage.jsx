import React from 'react';
import './LandingPage.css';

const pains = [
  ['Patients wait without answers', 'A planned surgery or a discharge can hang on a decision that nobody can explain in plain words.'],
  ['Paperwork goes back and forth', 'Missing details and unclear forms trigger rounds of queries between the hospital and the insurer.'],
  ['Reviewers repeat the same checks', 'Waiting periods, limits and co-pay are checked by hand, claim after claim, before the real medical judgement starts.'],
];

const steps = [
  ['Pick your plan', 'Choose individual, family floater or senior citizen, then a Basic or Comprehensive plan.'],
  ['Describe the treatment', 'Add the diagnosis, estimated cost, admission date and policy start date.'],
  ['Rules and AI check the claim', 'Waiting periods, sum insured and co-pay are calculated by rules. The AI reads the policy wording for everything else.'],
  ['Get a decision you can read', 'Approved, rejected, or sent to a reviewer, with the clauses behind it.'],
];

const features = [
  ['Plain-language rationale', 'Every decision explains itself in a few sentences.'],
  ['Clause citations', 'The exact policy clauses behind a decision are listed, so nothing is a black box.'],
  ['Rules before AI', 'Money and dates are calculated in code, so the AI never does the maths.'],
  ['Human review', 'Rejections and unclear cases go to a reviewer for the final call.'],
  ['Three plan types', 'Individual, family floater and senior citizen plans, each in Basic and Comprehensive tiers.'],
  ['Separate patient and reviewer access', 'Role-based sign-in keeps each view to what that person needs.'],
];

export default function LandingPage({ onGetStarted = () => {}, onSignIn = () => {} }) {
  return (
    <div className="lp" id="top">
      <header className="lp-nav">
        <div className="lp-wrap lp-nav-in">
          <a className="lp-logo" href="#top"><span>Medi</span>Auth</a>
          <nav className="lp-links" aria-label="Sections">
            <a href="#problem">The problem</a>
            <a href="#how">How it works</a>
            <a href="#features">What you get</a>
            <a href="#about">About</a>
          </nav>
          <div className="lp-nav-cta">
            <button className="lp-btn lp-btn-ghost" onClick={onSignIn}>Sign in</button>
            <button className="lp-btn lp-btn-solid" onClick={onGetStarted}>Get started</button>
          </div>
        </div>
      </header>

      <section className="lp-wrap lp-hero">
        <div className="lp-hero-copy">
          <h1>Know what your policy covers before the hospital asks for a deposit.</h1>
          <p className="lp-lead">
            MediAuth checks a planned treatment against your plan's wording and returns a clear,
            explained pre-authorization decision in seconds.
          </p>
          <div className="lp-actions">
            <button className="lp-btn lp-btn-solid lp-btn-lg" onClick={onGetStarted}>Try a sample claim</button>
            <a className="lp-btn lp-btn-ghost lp-btn-lg" href="#how">See how it works</a>
          </div>
        </div>

        <div className="lp-stage">
          <article className="lp-card" aria-label="Sample decision">
            <div className="lp-card-head">
              <div>
                <h2>Cataract surgery</h2>
                <p>Comprehensive Senior Citizen Plan</p>
              </div>
              <span className="lp-chip">Approved</span>
            </div>
            <p className="lp-card-text">
              Cataract is covered after the 12-month specific illness wait, which this policy has completed.
              A 10% co-pay applies.
            </p>
            <blockquote className="lp-clause">
              <strong>Clause 5.</strong> The insured bears 10% of every admissible claim amount.
            </blockquote>
            <dl className="lp-math">
              <div><dt>Estimated cost</dt><dd>₹85,000</dd></div>
              <div><dt>Co-pay (10%)</dt><dd>−₹8,500</dd></div>
              <div className="lp-total"><dt>Payable by insurer</dt><dd>₹76,500</dd></div>
            </dl>
            <p className="lp-card-foot">Sample decision. Checked by rules and AI, open to human override.</p>
          </article>
        </div>
      </section>

      <section className="lp-sec lp-paper" id="problem">
        <div className="lp-wrap">
          <h2 className="lp-h2">Pre-authorization is where cashless treatment gets stuck</h2>
          <p className="lp-intro">
            Before a planned hospital stay, the insurer has to confirm the treatment is covered. That approval
            is pre-authorization. Without it, patients pay upfront and claim later, or admissions get delayed.
          </p>
          <ul className="lp-pains">
            {pains.map(([t, d]) => (
              <li key={t}><h3>{t}</h3><p>{d}</p></li>
            ))}
          </ul>
        </div>
      </section>

      <section className="lp-sec" id="how">
        <div className="lp-wrap">
          <h2 className="lp-h2">From plan to decision in four steps</h2>
          <ol className="lp-steps">
            {steps.map(([t, d], i) => (
              <li key={t}><span className="lp-num">{i + 1}</span><h3>{t}</h3><p>{d}</p></li>
            ))}
          </ol>
        </div>
      </section>

      <section className="lp-sec lp-paper" id="features">
        <div className="lp-wrap">
          <h2 className="lp-h2">A decision you can read, check and overrule</h2>
          <div className="lp-feats">
            {features.map(([t, d]) => (
              <div key={t}><h3>{t}</h3><p>{d}</p></div>
            ))}
          </div>
        </div>
      </section>

      <section className="lp-sec" id="about">
        <div className="lp-wrap">
          <h2 className="lp-h2">About the creator</h2>
          <p className="lp-intro">Get in touch or find me on GitHub and LinkedIn.</p>
          <div className="lp-about-links">
            <a href="mailto:mursalinhq1@gmail.com">
              <span>Email</span>
              <strong>mursalinhq1@gmail.com</strong>
            </a>
            <a href="https://github.com/Mursalin7" target="_blank" rel="noreferrer">
              <span>GitHub</span>
              <strong>Mursalin7</strong>
            </a>
            <a href="https://www.linkedin.com/in/mursalin-haque-a34837163/" target="_blank" rel="noreferrer">
              <span>LinkedIn</span>
              <strong>Mursalin Haque</strong>
            </a>
          </div>
        </div>
      </section>

      <section className="lp-sec lp-cta">
        <div className="lp-wrap">
          <h2>See a decision for yourself</h2>
          <p>Sign in with a demo account and submit a sample claim.</p>
          <button className="lp-btn lp-btn-light lp-btn-lg" onClick={onGetStarted}>Try a sample claim</button>
        </div>
      </section>

      <footer className="lp-foot">
        <div className="lp-wrap">
          <p>MediAuth is a portfolio demo. Plans and rules are illustrative and not affiliated with any insurer.</p>
          <p>Built with React, Spring Boot, PostgreSQL and Groq.</p>
        </div>
      </footer>
    </div>
  );
}
