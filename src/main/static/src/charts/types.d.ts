type ValueOf<T> = T[keyof T];

type KeyExists<U, U1> = [U1] extends [keyof U] ? true : false;

type Propsable = {
  FC: React.FC;
  C: React.Component;
  CC: React.ComponentClass<any>;
  F: (...args: any) => any;
};
type PropsOfFC<C extends Propsable["FC"]> = {
  [K in keyof C["propTypes"]]: C["propTypes"][K] extends React.Validator<
      infer P
    >
    ? P
    : K;
};
type PropsOfF<C extends Propsable["F"]> = Parameters<C>[0];
type PropsOfC<C extends Propsable["C"]> = C extends React.Component<infer P>
  ? P
  : never;
type PropsOfCC<C extends Propsable["CC"]> = C extends React.ComponentClass<
    infer P
  >
  ? P
  : never;

type PropsOf<C extends ValueOf<Propsable>> = C extends Propsable["FC"]
  ? PropsOfFC<C>
  : C extends Propsable["C"]
    ? PropsOfC<C>
    : C extends Propsable["CC"]
      ? PropsOfCC<C>
      : C extends Propsable["F"]
        ? PropsOfF<C>
        : any;
