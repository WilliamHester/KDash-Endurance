import React from "react";
import './VariableBox.css';

export default function VariableBox({ title, children }) {
  return (
    <div className="variable-box-container">
      <div className="variable-box-title">{title}</div>
      <div className="variable-box-contents">{children}</div>
    </div>
  );
};

export function TextBox({ title, children }) {
  return <VariableBox title={title} className="text-box"><span className="variable-text">{children}</span></VariableBox>;
};
