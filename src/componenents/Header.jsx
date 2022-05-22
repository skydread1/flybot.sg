import { useState } from "react";
import { Link } from 'react-router-dom';

function Header() {
    const [navbarOpen, setNavbarOpen] = useState(false);

    const handleToggle = () => {
        setNavbarOpen(!navbarOpen)
    }
    return (
        <header>
            <div className="container flex justify-between items-center p-6 mx-auto
        border-b border-green-400">
                {/* Company Name */}
                <div className="text-lg font-bold text-blue-500">
                    (Flybot Pte Ltd)
                </div>

                {/* Menu items */}
                <nav className="text-lg text-blue-400 hidden lg:flex lg:justify-evenly lg:space-x-10">
                    <p className="hidden lg:inline-block">[</p>
                    <Link to="/" className="block mt-4 lg:inline-block hover:text-gray-700 lg:mt-0">
                        Home
                    </Link>
                    <Link to="/apply" className="block mt-4 lg:inline-block hover:text-gray-700 lg:mt-0">
                        Apply
                    </Link>
                    <a href="#footer-contact" className="block mt-4 hover:text-gray-700 lg:inline-block lg:mt-0">
                        Contact
                    </a>
                    <p className="hidden lg:inline-block">]</p>
                </nav>

                {/* Hamburger icon */}
                <div className="lg:hidden">
                    <button id="buger-button"
                        onClick={handleToggle}
                        className="flex items-center px-4 py-3 border rounded text-blue-400 border-blue-400 focus:outline-none">

                        <svg className="fill-blue-400 h-3 w-3" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
                            <title>Menu</title>
                            <path d="M0 3h20v2H0V3zm0 6h20v2H0V9zm0 6h20v2H0v-2z" />
                        </svg>
                    </button>
                </div>
            </div>

            {/* Menu items Mobile */}
            <nav id="myLinks" className={"container text-lg text-blue-500 text-center mx-auto border-b border-green-400 bg-blue-50 "
            + (navbarOpen ? 'block' : 'hidden')}>
                <Link to="/" className="block p-4 border border-green-500
            hover:bg-white hover:border-blue-900 hover:text-blue-900">
                    Home
                </Link>
                <Link to="/apply" className="block p-4 border border-green-500
            hover:bg-white hover:border-blue-900 hover:text-blue-900">
                    Apply
                </Link>
                <a href="#footer-contact" className="block p-4 border border-green-500
            hover:bg-white hover:border-blue-900 hover:text-blue-900">
                    Contact
                </a>
            </nav>
        </header>
    )
}

export default Header