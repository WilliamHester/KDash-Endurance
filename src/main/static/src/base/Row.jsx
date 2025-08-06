import React from "react";


export default function Row({children, style}) {
  return <div className="row" style={style}>{children}</div>;
};
