function Footer() {
    return (
        <footer id="footer-contact">
            <div className="container lg:flex lg:justify-evenly mx-auto
                            border-t border-green-400 text-center lg:text-left">
                <div className="m-10">
                    <h3 className="text-lg text-blue-400">Address</h3>
                    <p>1 Commonwealth Lane</p>
                    <p>#08-14</p>
                    <p>One Commonwealth</p>
                    <p>Singapore 149544</p>
                </div>
                <div className="m-10">
                    <h3 className="text-lg text-blue-400">Business Hours</h3>
                    <p>Monday - Friday, 08:30 - 17:00</p>
                </div>
                <div className="m-10">
                    <h3 className="text-lg text-blue-400">Email</h3>
                    <p>zhengliming@basecity.com</p>
                </div>
            </div>
        </footer>
    );
}

export default Footer