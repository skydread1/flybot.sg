import Header from "../componenents/Header";
import Footer from "../componenents/Footer";
import CLojureLogo from "../assets/clojure-logo.svg";
import LambdaLogo from "../assets/lambda-logo.svg";
import SuitsImage from "../assets/4suits.svg";
import BinaryImage from "../assets/binary.svg";

function Home() {
    return (
        <div>
            <Header />
            <section className="container mx-auto">
                {/* Clojure */}
                <div
                    className="lg:flex lg:space-x-10 w-full justify-center items-center p-10 border-b lg:border-r border-green-400">
                    <div className="hidden lg:block first-line:w-full lg:w-1/4 pb-5">
                        <img src={CLojureLogo} alt="Clojure logo" />
                    </div>
                    <div className="w-full lg:w-2/4">
                        <h2 className="text-lg font-bold text-center lg:text-left text-blue-500 lg:pb-3">
                            {'{:our-language "Clojure"}'}
                        </h2>

                        <div className="lg:hidden w-52 mx-auto p-5">
                            <img src={CLojureLogo} alt="Clojure logo" />
                        </div>

                        <p className="pb-3"> We use
                            <a href="https://clojure.org/"
                                className="underline text-blue-400"
                                target='_blank'
                                rel="noreferrer"
                            > Clojure </a>
                            Clojure as our main programming language for development.</p>
                        <p>In short, clojure is:</p>
                        <ul className="pb-3 text-left">
                            <li className="pl-3">- a Functional Programming language</li>
                            <li className="pl-3">- a member of the Lisp family of languages</li>
                            <li className="pl-3">- has a powerful runtime polymorphism</li>
                            <li className="pl-3">- simplifies multi-threaded programming</li>
                            <li className="pl-3">- hosted on the JVM</li>
                            <li className="pl-3">- a dynamic environment</li>
                        </ul>
                    </div>
                </div>

                {/* Functional Programming */}
                <div
                    className="lg:flex lg:space-x-10 w-full justify-center items-center p-10 border-b bg-blue-50 lg:border-l border-green-400">
                    <div className="w-full lg:w-2/4">
                        <h2 className="text-lg font-bold text-center lg:text-left text-blue-500 lg:pb-3">
                            {'{:paradigms ["DOP" "FP"]}'}
                        </h2>

                        <div className="lg:hidden w-36 mx-auto p-5">
                            <img src={LambdaLogo} alt="Lamdba logo" />
                        </div>

                        <p className="pb-3">We use the Data Oriented Programming (DOP) and functional programming (FP) paradigms to
                            implement our diverse projects.</p>
                        <p className="pb-3">Indeed Clojure supports and relies on both of these concepts.</p>
                        <p className="pb-3">DOP evolves around the idiom "Everything as data". It is about building abstraction
                            around basic data structures (list, maps, vectors etc.).</p>
                        <p className="pb-3">You can view both DOP and FP as opposition to Object Oriented Programming (OOP).</p>
                    </div>
                    <div className="hidden lg:block w-full lg:w-1/4">
                        <img className="w-1/2" src={LambdaLogo} alt="Lamdba logo" />
                    </div>
                </div>

                {/* Client */}
                <div
                    className="lg:flex lg:space-x-10 w-full justify-center items-center p-10 border-b lg:border-r border-green-400">
                    <div className="hidden lg:block lg:w-1/4 pb-5">
                        <img className="w-full" src={SuitsImage} alt="Card suits" />
                    </div>
                    <div className="w-full lg:w-2/4">
                        <h2 className="text-lg font-bold text-center lg:text-left text-blue-500 lg:pb-3">
                            {'{:our-client "Golden Island"}'}
                        </h2>

                        <div className="lg:hidden w-52 mx-auto p-5">
                            <img src={SuitsImage} alt="Card suits" />
                        </div>

                        <p className="pb-3">We provide technical support and solutions to clients who run 18 games in total in the platform
                            <a href="https://www.80166.com/"
                                className="underline text-blue-400"
                                target='_blank'
                                rel="noreferrer"
                            > Golden Island</a>.
                        </p>
                        <p className="pb-3">Lots of the server-side code base is written in Clojure such as user account,
                            authentification, coins top up, message, activity, tasks/rewards, data analysis and some web pages.
                        </p>
                    </div>
                </div>

                {/* Magic */}
                <div
                    className="lg:flex lg:space-x-10 w-full justify-center items-center p-10 bg-blue-50 lg:border-l border-green-400">
                    <div className="w-full lg:w-2/4">
                        <h2 className="text-lg font-bold text-center lg:text-left text-blue-500 lg:pb-3">
                            {'{:project "Clojure in Unity"}'}
                        </h2>

                        <div className="lg:hidden w-32 mx-auto">
                            <img src={BinaryImage} alt="Binary logo" />
                        </div>

                        <p>Clojure can run on different platform:</p>
                        <p className="pb-3">Java (Clojure) - JavaScript (ClojureScript) - CLR (ClojureCLR)</p>
                        <p className="pb-3">However, the ClojureCLR does not work with Unity as it has limited control over the
                            generated dlls and IL2CPP for iOS is not allowed with the DLR used by ClojureCLR.</p>
                        <p className="pb-3">Hence the
                            <a href="https://github.com/nasser/magic"
                                className="underline text-blue-400"
                                target='_blank'
                                rel="noreferrer"
                                aria-label='Github'
                            > MAGIC </a>
                            bootstrapped compiler written in Clojure targeting the CLR. We are now
                            able to compile Clojure libraries easily to dlls and import and use them in our Unity games.</p>
                        <p>We are currently working on:</p>
                        <ul className="pb-3 text-left">
                            <li className="pl-3">- improving the performance of the compiler</li>
                            <li className="pl-3">- improving the dependencies management to easily import Clojure libraries to
                                Unity projects via
                                <a href="https://github.com/nasser/nostrand"
                                    className="underline text-blue-400"
                                    target='_blank'
                                    rel="noreferrer"
                                    aria-label='Github'
                                > Nostrand </a>
                            </li>
                            <li className="pl-3">- integrate Clojure directly to Unity using the Entity Component System (ECS)</li>
                        </ul>
                    </div>
                    <div className="hidden lg:block w-full lg:w-1/4">
                        <img className="w-full" src={BinaryImage} alt="Binary logo" />
                    </div>
                </div>
            </section>
            <Footer />
        </div>
    );
}

export default Home