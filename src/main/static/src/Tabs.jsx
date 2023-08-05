import React from "react";
import "./Tabs.css";

class Tabs extends React.Component {

  constructor(props) {
    super(props)

    this.onClickTabItem = this.onClickTabItem.bind(this);
    this.state = {
      activeTab: this.props.children[0].props.label,
    };
  }

  onClickTabItem(tab) {
    this.setState({ activeTab: tab });
  }

  render() {
    const activeTab = this.state.activeTab;
    const onClickTabItem = this.onClickTabItem;
    var children = this.props.children;

    return (
      <div className="tabs">
      <ol className="tab-list">
        {children.map((child) => {
          const { label } = child.props;

          return (
            <Tab
              activeTab={activeTab}
              key={label}
              label={label}
              onClick={onClickTabItem}
            />
          );
        })}
      </ol>
      <div className="tab-content">
        {children.map((child) => {
          if (child.props.label !== activeTab) {
            return undefined;
          }
          return child.props.children;
        })}
      </div>
    </div>
    )
  }
}

class Tab extends React.Component {
  constructor(props) {
    super(props);

    this.onClick = this.onClick.bind(this);
  }

  onClick() {
    const { label, onClick } = this.props;
    onClick(label);
  }

  render() {
    const {
      onClick,
      props: {
        activeTab,
        label,
      },
    } = this;

    let className = 'tab-list-item';

    if (activeTab === label) {
      className += ' tab-list-active';
    }

    return (
      <li
        className={className}
        onClick={onClick}
      >
        {label}
      </li>
    );
  }
}

export default Tabs;