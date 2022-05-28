function Footer() {
    return (
        <footer id='footer-contact' >
            <div className='container lg:flex lg:justify-evenly mx-auto p-6
                            border-t border-green-400 text-center lg:text-left'>
                <div className='m-10'>
                    <h3 className='text-lg font-bold text-sky-400
                    dark:text-sky-200'>Address</h3>
                    <p>1 Commonwealth Lane</p>
                    <p>#08-14</p>
                    <p>One Commonwealth</p>
                    <p>Singapore 149544</p>
                </div>
                <div className='m-10'>
                    <h3 className='text-lg font-bold text-sky-400
                    dark:text-sky-200'>Business Hours</h3>
                    <p>Monday - Friday, 08:30 - 17:00</p>
                </div>
                <div className='m-10'>
                    <h3 className='text-lg font-bold text-sky-400
                    dark:text-sky-200'>Contact</h3>
                    <p>zhengliming@basecity.com</p>
                    <a href='https://www.linkedin.com/company/86215279/'
                        className='underline text-sky-400'
                        target='_blank'
                        rel='noreferrer'>
                        LinkedIn
                    </a>
                </div>
            </div>
        </footer>
    );
}

export default Footer