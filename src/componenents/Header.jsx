import { useState } from "react";
import { Link } from 'react-router-dom';
import useDarkMode from '../hooks/useDarkMode'
import flybotLogo from '../assets/flybot-logo.png'

function Header() {
    // dark mode hook
    const [nextTheme, setTheme] = useDarkMode()

    // mobile navbar hook
    const [navbarOpen, setNavbarOpen] = useState(false);
    const handleToggle = () => {
        setNavbarOpen(!navbarOpen)
    }
    const closeMenu = () => {
        setNavbarOpen(false)
    }
    return (
        <header>
            <div className="container flex justify-between items-center p-6 mx-auto border-b border-green-500">
                {/* Company Name */}
                <div>
                    <img src={flybotLogo} alt="FLybot logo"
                        className="w-16 lg:w-28"/>
                </div>

                {/* Dark Mode switch*/}
                <div className="cursor-pointer group"
                    onClick={() => setTheme(nextTheme)}>
                    {nextTheme === 'light'
                        ?
                        <svg xmlns="http://www.w3.org/2000/svg"
                            className="w-8 fill-sky-200 group-hover:fill-yellow-200 group-hover:animate-pulse"
                            viewBox="0 0 20 20">
                            <path d="M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z" />
                        </svg>
                        :
                        <svg xmlns="http://www.w3.org/2000/svg"
                            className="w-8 fill-sky-400 group-hover:fill-red-400 group-hover:animate-pulse"
                            viewBox="0 0 20 20">
                            <path fillRule="evenodd" d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z" clipRule="evenodd" />
                        </svg>
                    }
                </div>

                {/* Menu items */}
                <nav className="text-lg text-sky-400 hidden lg:flex lg:justify-evenly lg:space-x-10 
                dark:text-sky-200">
                    <p className="hidden lg:inline-block">[</p>
                    <Link to="/" className="block mt-4 lg:inline-block hover:text-zinc-900 lg:mt-0
                    hover:animate-pulse
                    dark:hover:text-white">
                        Home
                    </Link>
                    <Link to="/apply" className="block mt-4 lg:inline-block hover:text-zinc-900 lg:mt-0
                    hover:animate-pulse
                    dark:hover:text-white">
                        Apply
                    </Link>
                    <a href="#footer-contact" className="block mt-4 hover:text-zinc-900 lg:inline-block lg:mt-0
                    hover:animate-pulse
                    dark:hover:text-white">
                        Contact
                    </a>
                    <p className="hidden lg:inline-block">]</p>
                </nav>

                {/* Hamburger icon */}
                <div className="lg:hidden">
                    <button id="buger-button"
                        onClick={handleToggle}
                        className="flex items-center px-4 py-3 border rounded border-sky-400 text-sky-400 focus:outline-none
                        dark:border-sky-200 dark:text-sky-200">
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                            <path fillRule="evenodd" d="M3 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM3 10a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM3 15a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clipRule="evenodd" />
                        </svg>
                    </button>
                </div>
            </div>

            {/* Menu items Mobile */}
            <nav className={"container text-lg text-sky-500 text-center mx-auto bg-sky-50 dark:bg-zinc-800 "
                + (navbarOpen ? 'block' : 'hidden')}
                onClick={() => closeMenu()}>
                <Link to="/"
                    className="block p-4 border-x border-b border-green-500
            hover:bg-white hover:text-sky-900
            dark:text-sky-200 dark:hover:bg-zinc-500 dark:hover:text-white">
                    Home
                </Link>
                <Link to="/apply"
                    className="block p-4 border-x border-green-500
            hover:bg-white hover:text-sky-900
            dark:text-sky-200 dark:hover:bg-zinc-500 dark:hover:text-white">
                    Apply
                </Link>
                <a href="#footer-contact"
                    className="block p-4 border border-green-500
            hover:bg-white hover:text-sky-900
            dark:text-sky-200 dark:hover:bg-zinc-500 dark:hover:text-white">
                    Contact
                </a>
            </nav>
        </header>
    )
}

export default Header