import Head from "./components/Head"
import Header from "./components/Header";
import Footer from "./components/Footer";
import Home from "./pages/Home";
import Apply from "./pages/Apply";
import About from "./pages/About";
import { Route, Routes, BrowserRouter } from 'react-router-dom';

function App() {
  return (
    <div className="bg-zinc-50 text-zin-900 min-h-screen
    dark:bg-zinc-900 dark:text-sky-50">
      <Head />
      <BrowserRouter>
          <Header />
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/apply" element={<Apply />} />
            <Route path="/about" element={<About />} />
          </Routes>
          <Footer />
      </BrowserRouter>
    </div>

  );
}

export default App;
