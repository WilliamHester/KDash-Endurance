import React, { useState, useRef, useEffect } from 'react';
import Daytona from './daytona.svg';
import './TrackMapPage.css';

export default function TrackMapPage() {
  // Ref to access the <path> element directly
  const pathRef = useRef(null);
  const container = useRef(null);

  // State to hold the calculated coordinates for the dot
  const [point, setPoint] = useState({ x: 0, y: 0 });

  // State for the slider, representing percentage along the path
  const [progress, setProgress] = useState(0); // Start at 50%

  useEffect(() => {
    if (container.current === null) return;
    const pathElement = container.current.querySelector('path');
    if (pathElement) {
      console.log('found path');
      pathRef.current = pathElement;
    } else {
      console.log('did not find path');
    }

    let percent = 0;

    const intervalId = setInterval(() => {
      if (pathRef.current) {
        const path = pathRef.current;
        const totalLength = path.getTotalLength();
        console.log('totalLength: ', totalLength);

        // Calculate the distance along the path based on the progress percentage
        const distance = (totalLength * percent) / 100;
        percent += 0.1;

        // Get the {x, y} coordinates at that distance
        const coordinates = path.getPointAtLength(distance);

        setPoint({ x: coordinates.x, y: coordinates.y });
      }
    }, 20);

    return () => clearInterval(intervalId); // Cleanup interval on unmount
  }, [progress, container]); // Re-run this effect whenever 'progress' changes

  return (
    <div>
      <div className="svg-wrapper" ref={container}>
        <Daytona width={300} height={300} />
        <svg className="overlay-svg">
          <circle
            cx={point.x}
            cy={point.y}
            r="8" /* radius of the dot */
            fill="#FF6347" /* color of the dot */
          />
        </svg>
      </div>
    </div>
  );
}
