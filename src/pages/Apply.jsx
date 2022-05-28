import Head from '../components/Head';

function Apply() {
    return (
        <section className='container mx-auto'>
            <Head
                title='Flybot - Apply'
                description='We are hiring Clojure developers or fresh graduates as full-timers or interns.'
                keywords='Clojure, Development, Singapore, Job, Hiring, Offer'>
            </Head>

            {/* Job description */}
            <div className='w-full p-10 border-b lg:border-r border-green-400
                hover:shadow-md hover:border relative z-50
              dark:shadow-green-400'>
                <div className='w-full'>
                    <h2 className='text-lg font-bold text-center lg:text-left text-sky-500 pb-3
                        dark:text-sky-200'>
                        {'{:job "Description"}'}
                    </h2>
                    <p>As an software engineer (full-time or intern) at Flybot, you will:</p>
                    <ul className='list-disc list-inside pb-3 text-left'>
                        <li className='pl-3'>Work on Clojure backend apps for mobile games</li>
                        <li className='pl-3'>Work on improving the workflow to ease the release of new apps in the future
                        </li>
                        <li className='pl-3'>Take part in a data-oriented experimental stack using Clojure backend libs
                            integrated in Unity</li>
                        <li className='pl-3'>Be exposed to lots of new technologies and frameworks</li>
                        <li className='pl-3'>Have a real software engineering experience requiring professionalism and
                            organizational skills</li>
                    </ul>
                </div>
            </div>

            {/* Qualifications */}
            <div className='w-full p-10 border-b bg-sky-50 lg:border-l border-green-400
                dark:bg-zinc-800
                hover:shadow-md hover:border relative z-40
              dark:shadow-green-400'>
                <div className='w-full'>
                    <h2 className='text-lg font-bold text-center lg:text-left text-sky-500 pb-3
                        dark:text-sky-200'>
                        {'{:job "Qualifications"}'}
                    </h2>
                    <p>What we look for in Flybot applicants:</p>
                    <ul className='list-disc list-inside pb-3 text-left'>
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
            <div className='w-full p-10 border-b lg:border-r border-green-400
                hover:shadow-md hover:border relative z-30
              dark:shadow-green-400'>
                <div className='w-full'>
                    <h2 className='text-lg font-bold text-center lg:text-left text-sky-500 pb-3
                        dark:text-sky-200'>
                        {'{:job "End Goal"}'}
                    </h2>
                    <p className='pb-3'>If you are an intern, we hope you will carry on with us for a full-time position in the
                        future (depending on candidate performance and motivation).</p>
                    <p>If you are a full-timer, we hope you will gain good knowledge and independence becoming an efficient
                        software engineer who will provide good value to our company.</p>
                </div>
            </div>

            {/* Application Method */}
            <div className='w-full p-10 bg-sky-50 border-b lg:border-l border-green-400
                dark:bg-zinc-800
                hover:shadow-md hover:border relative z-20
              dark:shadow-green-400'>
                <div className='w-full'>
                    <h2 className='text-lg font-bold text-center lg:text-left text-sky-500 pb-3
                        dark:text-sky-200'>
                        {'{:job "Application"}'}
                    </h2>
                    <p className='pb-3'>Please fill this google form:</p>
                    <a href='https://docs.google.com/forms/d/e/1FAIpQLScq-J0zaqLhWYtllUkBL3OpY-t7OiqJEKPJHsbEKvM3EB1lbg/viewform'
                        className='block text-center lg:w-1/4 p-4 rounded bg-sky-900 border border-white text-white
                            focus:outline-none hover:border-black hover:text-black'
                        target='_blank' rel='noreferrer'>
                        APPLY
                    </a>
                </div>
            </div>
        </section>
    );
}

export default Apply