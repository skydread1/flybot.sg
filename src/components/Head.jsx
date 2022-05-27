import { Helmet } from 'react-helmet';

function Head({ title, description, keywords }) {
  return (
    <Helmet>
      {/* General */}
      <title>{title}</title>
      <meta name="description" content={description} />
      <meta name="keywords" content={keywords} />
    </Helmet>
  )
}

Head.defaultProps = {
  title: "Flybot",
  description: "Software company leveraging the functional programming language Clojure. We design and implement systems to solve complex problems in a simple and scalable way.",
  keywords: "Clojure, Functional Programming, Development, Singapore, Code"
};

export default Head