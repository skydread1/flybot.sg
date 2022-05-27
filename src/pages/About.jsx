import Head from '../components/Head'
import flybotLogo from '../assets/flybot-logo.png'
import GitHubLogo from '../assets/github-mark-logo.png'
import GitHubLogoDark from '../assets/github-mark-logo-dark-mode.png'
import LinkedinLogo from '../assets/linkedin-logo.png'

function About() {
    return (
        <section className="container mx-auto">
            <Head
                title="Flybot - About Us"
                description="We are a high-tech software development firm with the mission of providing the most advanced technological services and vision of serving clients all over the globe."
                keywords="Clojure, Development, Singapore, About, Presentation, Team">
            </Head>
            {/* The company */}
            <div
                className="lg:flex lg:space-x-10 w-full justify-center items-center p-10 border-b lg:border-r border-green-400
                    group hover:shadow-md hover:border relative z-50
                    dark:shadow-green-400">
                <div className="hidden lg:block w-full lg:w-1/4 group-hover:animate-pulse">
                    <img src={flybotLogo} alt="FLybot logo"
                        className="w-full" />
                </div>
                <div className="w-full lg:w-2/4">
                    <h2 className="text-lg font-bold text-center lg:text-left text-sky-500 lg:pb-3
                        dark:text-sky-200">
                        {'{:company "Flybot"}'}
                    </h2>

                    {/* Image for Mobile */}
                    <div className="lg:hidden mx-auto p-5">
                        <img src={flybotLogo} alt="FLybot logo"
                            className="w-1/2 mx-auto" />
                    </div>

                    <p className="pb-3">Flybot Pte Ltd was established in 2015 in Singapore.</p>
                    <p className="pb-3">We are a high-tech software development firm with the mission of providing the most advanced technological services and vision of serving clients all over the globe.</p>
                    <p className="pb-3">We leverage the programming language Clojure to design and implement systems to solve complex problems in a simple and scalable way.</p>
                </div>
            </div>

            {/* The team */}
            <div
                className="w-full p-10 border-b bg-sky-50 lg:border-l border-green-400
                    group hover:shadow-md hover:border relative z-40
                    dark:bg-zinc-800 dark:shadow-green-400">
                <h2 className="text-lg font-bold  text-sky-500 text-center
                        dark:text-sky-200">
                    {'{:team "members"}'}
                </h2>
                {/* Team Members*/}
                <div class="container lg:flex lg:justify-evenly mx-auto text-center">
                    <div className="lg:w-1/3 p-5">
                        <h3 className="font-bold">Luo Tian</h3>
                        <h2 className="pb-4">CEO</h2>
                        <a href="https://github.com/robertluo"
                            target='_blank'
                            rel="noreferrer">
                            <img src={GitHubLogo} alt="Github Mark logo"
                                className="dark:hidden w-12 mx-auto hover:animate-pulse" />
                            <img src={GitHubLogoDark} alt="Github Mark logo"
                                className="hidden dark:block w-12 mx-auto hover:animate-pulse" />
                        </a>
                    </div>
                    <div className="lg:w-1/3 p-5">
                        <h3 className="font-bold">Loic Blanchard</h3>
                        <h2 className="pb-4">Software Engineer</h2>
                        <a href="https://github.com/skydread1"
                            target='_blank'
                            rel="noreferrer">
                            <img src={GitHubLogo} alt="Github Mark logo"
                                className="dark:hidden w-12 mx-auto hover:animate-pulse" />
                            <img src={GitHubLogoDark} alt="Github Mark logo"
                                className="hidden dark:block w-12 mx-auto hover:animate-pulse" />
                        </a>
                    </div>
                    <div className="lg:w-1/3 p-5">
                        <h3 className="font-bold">Melinda Zheng</h3>
                        <h2 className="pb-4">HR Manager</h2>
                        <a href="https://www.linkedin.com"
                            target='_blank'
                            rel="noreferrer">
                            <img src={LinkedinLogo} alt="LinkedIn logo"
                                className="w-12 mx-auto hover:animate-pulse" />
                        </a>
                    </div>
                </div>
            </div>
        </section>
    )
}

export default About