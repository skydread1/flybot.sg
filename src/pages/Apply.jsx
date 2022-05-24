function Apply() {
    return (
        <div>
            <section className="container mx-auto">
                {/* Job description */}
                <div className="w-full p-10 border-b lg:border-r border-green-400
                hover:shadow-lg dark:shadow-sky-200">
                    <div className="w-full">
                        <h2 className="text-lg font-bold text-center lg:text-left text-sky-500 pb-3">
                            Job description
                        </h2>
                        <p>As an software engineer at Flybot, you will:</p>
                        <ul className="pb-3 text-left">
                            <li className="pl-3">- Work on Clojure backend apps for mobile games</li>
                            <li className="pl-3">- Work on improving the workflow to ease the release of new apps in the future
                            </li>
                            <li className="pl-3">- Take part in a data-oriented experimental stack using Clojure backend libs
                                integrated in Unity</li>
                            <li className="pl-3">- Be exposed to lots of new technologies and frameworks</li>
                            <li className="pl-3">- Have real software engineering experience requiring professionalism and
                                organisational skills</li>
                        </ul>
                    </div>
                </div>

                {/* Qualifications */}
                <div className="w-full p-10 border-b bg-sky-50 lg:border-l border-green-400
                dark:bg-zinc-800 hover:shadow-lg dark:shadow-sky-200">
                    <div className="w-full pb-5">
                        <h2 className="text-lg font-bold text-center lg:text-left text-sky-500 pb-3">
                            Qualifications
                        </h2>
                        <p>What we look for in Flybot applicants:</p>
                        <ul className="list-disc list-inside pb-3 text-left">
                            <li>Computer Science major</li>
                            <li>Willing to learn the functional programming language Clojure</li>
                            <li>Proficiency in one or more of the following developer skills: Clojure, Java,
                                JavaScript, Scala, Haskell, C/C++, PHP, Python, Ruby</li>
                            <li>Familiar with functional programming concepts is a plus.</li>
                            <li>Machine Learning, and Artificial Intelligence experience are a plus.</li>
                            <li>API design skills</li>
                            <li>At least B2 level in English</li>
                        </ul>
                    </div>
                </div>

                {/* End goal */}
                <div className="w-full p-10 border-b lg:border-r border-green-400
                hover:shadow-lg dark:shadow-sky-200">
                    <div className="w-full pb-5">
                        <h2 className="text-lg font-bold text-center lg:text-left text-sky-500 pb-3">
                            End goal
                        </h2>
                        <p className="pb-3">If you are intern, we hope you will carry on with us for a full-time position in the
                            future (depending on candidate performance and motivation).</p>
                        <p>If you are a full-timer, we hope you will gain good knowledge and independence becoming an efficient
                            software engineer who will provide good value to our company.</p>
                    </div>
                </div>

                {/* Application Method */}
                <div className="w-full p-10 bg-sky-50 border-b lg:border-l border-green-400
                dark:bg-zinc-800 hover:shadow-lg dark:shadow-sky-200">
                    <div className="w-full">
                        <h2 className="text-lg font-bold text-center lg:text-left text-sky-500 pb-3">
                            Application Method
                        </h2>
                        <p className="pb-3">Please fill this google form:</p>
                        <a href="https://docs.google.com/forms/d/e/1FAIpQLScq-J0zaqLhWYtllUkBL3OpY-t7OiqJEKPJHsbEKvM3EB1lbg/viewform"
                            className="block text-center p-5 rounded bg-sky-900 border border-white text-white
                            focus:outline-none shadow-lg hover:shadow-blue-200"
                            target="_blank" rel="noreferrer">
                            APPLICATION FOR EMPLOYMENT
                        </a>
                    </div>
                </div>
            </section>
        </div>
    );
}

export default Apply