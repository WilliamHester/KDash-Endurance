import { writable } from 'svelte/store';
import { browser } from '$app/environment';

const defaultOptions = {
  targetStintLength: 20,
  fuelMargin: 1.0,
};

const storedOptions = browser ? window.localStorage.getItem('kdash-endurance-options') : null;

const initialOptions = storedOptions ? JSON.parse(storedOptions) : defaultOptions;

const options = writable(initialOptions);

if (browser) {
  options.subscribe(value => {
    window.localStorage.setItem('kdash-endurance-options', JSON.stringify(value));
  });
}

export default options;
