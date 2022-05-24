import Head from "./componenents/Head"
import Header from "./componenents/Header";
import Footer from "./componenents/Footer";
import Home from "./pages/Home";
import Apply from "./pages/Apply";
import { Route, Routes, BrowserRouter } from 'react-router-dom';

function App() {
  return (
    <div className="bg-zinc-50 text-zin-900
    dark:bg-zinc-900 dark:text-sky-50">
      <Head />
      <BrowserRouter>
          <Header />
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/apply" element={<Apply />} />
          </Routes>
          <Footer />
      </BrowserRouter>
    </div>

  );
}

export default App;
