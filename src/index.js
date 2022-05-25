import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';

const root = ReactDOM.createRoot(document.getElementById('root'));
// Sets dark mode by default on first visit
if (localStorage.theme === undefined){
    localStorage.setItem('theme', 'dark');
}

root.render(
    <App />
);
