import React from "react";
import "./LapRow.css";

class LapRow extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            value: null,
        };
    }

    render() {
        return (
            <li className="LapRow">
                <span>Row {this.props.value.speed}</span>
            </li>
        );
    }
}

export default LapRow;
