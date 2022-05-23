import Head from "./componenents/Head"
import Home from "./pages/Home";
import Apply from "./pages/Apply";
import { Route, Routes, BrowserRouter } from 'react-router-dom';

function App() {
  return (
    <div>
      <Head />
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/apply" element={<Apply />} />
        </Routes>
      </BrowserRouter>
    </div>

  );
}

export default App;
